/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.querydsl.binding;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.PropertyValues;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.Map.Entry;

/**
 * Builder assembling {@link Predicate} out of {@link PropertyValues}.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @since 1.11
 */
public class QuerydslPredicateBuilder {

	private static final String OPERATION_DELIMITER = ":";

	private final ConversionService conversionService;
	private final MultiValueWithOperationBinding<?, ?> defaultOpBinding;
	private final Map<PathInformation, Path<?>> paths;
	private final EntityPathResolver resolver;

	/**
	 * Creates a new {@link QuerydslPredicateBuilder} for the given {@link ConversionService} and
	 * {@link EntityPathResolver}.
	 *
	 * @param conversionService must not be {@literal null}.
	 * @param resolver          can be {@literal null}.
	 */
	public QuerydslPredicateBuilder(ConversionService conversionService, EntityPathResolver resolver) {

		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.defaultOpBinding = new QuerydslDefaultWithOperationBinding();
		this.conversionService = conversionService;
		this.paths = new HashMap<PathInformation, Path<?>>();
		this.resolver = resolver;
	}

	/**
	 * Creates a Querydsl {@link Predicate} for the given values, {@link QuerydslBindings} on the given
	 * {@link TypeInformation}.
	 *
	 * @param type     the type to create a predicate for.
	 * @param values   the values to bind.
	 * @param bindings the {@link QuerydslBindings} for the predicate.
	 * @return
	 */
	public Predicate getPredicate(TypeInformation<?> type, MultiValueMap<String, String> values,
								  QuerydslBindings bindings) {

		Assert.notNull(bindings, "Context must not be null!");

		BooleanBuilder builder = new BooleanBuilder();

		if (values.isEmpty()) {
			return builder.getValue();
		}

		for (Entry<String, List<String>> entry : values.entrySet()) {

			if (isSingleElementCollectionWithoutText(entry.getValue())) {
				continue;
			}

			PathAndOperation pathAndOperation = getPath(entry.getKey());

			if (!bindings.isPathAvailable(pathAndOperation.getPath(), type)) {
				continue;
			}

			PathInformation propertyPath = bindings.getPropertyPath(pathAndOperation.getPath(), type);

			if (propertyPath == null) {
				continue;
			}

			Collection<Object> value = convertToPropertyPathSpecificType(entry.getValue(), propertyPath);
			Predicate predicate = invokeBinding(propertyPath, bindings, value, pathAndOperation.getOperation());

			if (predicate != null) {
				builder.and(predicate);
			}
		}

		return builder.getValue();
	}

	/**
	 * Invokes the binding of the given values, for the given {@link PropertyPath} and {@link QuerydslBindings}.
	 *
	 * @param dotPath   must not be {@literal null}.
	 * @param bindings  must not be {@literal null}.
	 * @param values    must not be {@literal null}.
	 * @param operation must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private Predicate invokeBinding(PathInformation dotPath, QuerydslBindings bindings, Collection<Object> values, OperationType operation) {
		Path<?> path = getPath(dotPath, bindings);
		MultiValueBinding binding = bindings.getBindingForPath(dotPath);

		if (operation == null) {
			return binding != null ? binding.bind(path, values) :
					((MultiValueWithOperationBinding) defaultOpBinding).bind(path, values, OperationType.NO_OOP);
		} else {
			return ((MultiValueWithOperationBinding) defaultOpBinding).bind(path, values, operation);
		}
	}

	/**
	 * Returns the {@link Path} for the given {@link PropertyPath} and {@link QuerydslBindings}. Will try to obtain the
	 * {@link Path} from the bindings first but fall back to reifying it from the PropertyPath in case no specific binding
	 * has been configured.
	 *
	 * @param path     must not be {@literal null}.
	 * @param bindings must not be {@literal null}.
	 * @return
	 */
	private Path<?> getPath(PathInformation path, QuerydslBindings bindings) {

		Path<?> resolvedPath = bindings.getExistingPath(path);

		if (resolvedPath != null) {
			return resolvedPath;
		}

		resolvedPath = paths.get(resolvedPath);

		if (resolvedPath != null) {
			return resolvedPath;
		}

		resolvedPath = path.reifyPath(resolver);
		paths.put(path, resolvedPath);

		return resolvedPath;
	}

	/**
	 * Converts the given source values into a collection of elements that are of the given {@link PropertyPath}'s type.
	 * Considers a single element list with an empty {@link String} an empty collection because this basically indicates
	 * the property having been submitted but no value provided.
	 *
	 * @param source must not be {@literal null}.
	 * @param path   must not be {@literal null}.
	 * @return
	 */
	private Collection<Object> convertToPropertyPathSpecificType(List<String> source, PathInformation path) {

		Class<?> targetType = path.getLeafType();

		if (source.isEmpty() || isSingleElementCollectionWithoutText(source)) {
			return Collections.emptyList();
		}

		Collection<Object> target = new ArrayList<Object>(source.size());

		for (String value : source) {

			target.add(conversionService.canConvert(String.class, targetType)
					? conversionService.convert(value, TypeDescriptor.forObject(value), getTargetTypeDescriptor(path)) : value);
		}

		return target;
	}

	/**
	 * Returns the target {@link TypeDescriptor} for the given {@link PathInformation} by either inspecting the field or
	 * property (the latter preferred) to pick up annotations potentially defined for formatting purposes.
	 *
	 * @param path must not be {@literal null}.
	 * @return
	 */
	private static TypeDescriptor getTargetTypeDescriptor(PathInformation path) {

		PropertyDescriptor descriptor = path.getLeafPropertyDescriptor();

		Class<?> owningType = path.getLeafParentType();
		String leafProperty = path.getLeafProperty();

		if (descriptor == null) {
			return TypeDescriptor.nested(ReflectionUtils.findField(owningType, leafProperty), 0);
		}

		return TypeDescriptor
				.nested(new Property(owningType, descriptor.getReadMethod(), descriptor.getWriteMethod(), leafProperty), 0);
	}

	/**
	 * Returns whether the given collection has exactly one element that doesn't contain any text. This is basically an
	 * indicator that a request parameter has been submitted but no value for it.
	 *
	 * @param source must not be {@literal null}.
	 * @return
	 */
	private static boolean isSingleElementCollectionWithoutText(List<String> source) {
		return source.size() == 1 && !StringUtils.hasText(source.get(0));
	}

	private static PathAndOperation getPath(String requestPath) {
		String[] parts = requestPath.split(OPERATION_DELIMITER, 2);
		return new PathAndOperation(parts[0], parts.length == 1 ? null : OperationType.value(parts[1]));
	}

	@Data
	@AllArgsConstructor
	static class PathAndOperation {
		String path;
		OperationType operation;
	}
}
