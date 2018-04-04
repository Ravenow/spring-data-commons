/*
 * Copyright 2015-2017 the original author or authors.
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

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import org.hamcrest.Matcher;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.querydsl.QUser;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Christoph Strobl
 * @author Oliver Gierke
 */
public class QuerydslDefaultWithOperationBindingUnitTests {

	QuerydslDefaultWithOperationBinding binding;

	@Before
	public void setUp() {
		binding = new QuerydslDefaultWithOperationBinding();
	}

	@Test
	public void shouldCreatePredicateCorrectlyWhenPropertyIsInRoot() {
		Predicate predicate = binding.bind(QUser.user.firstname, Collections.singleton("tam"), OperationType.NO_OOP);
		assertPredicate(predicate, is(QUser.user.firstname.eq("tam")));
	}

	@Test
	public void shouldCreatePredicateCorrectlyWhenPropertyIsInNestedElement() {
		Predicate predicate = binding.bind(QUser.user.address.city, Collections.singleton("two rivers"), OperationType.NO_OOP);
		assertThat(predicate.toString(), is(QUser.user.address.city.eq("two rivers").toString()));
	}

	@Test
	public void shouldCreatePredicateWithContainingWhenPropertyIsCollectionLikeAndValueIsObject() {
		Predicate predicate = binding.bind(QUser.user.nickNames, Collections.singleton("dragon reborn"), OperationType.NO_OOP);
		assertPredicate(predicate, is(QUser.user.nickNames.contains("dragon reborn")));
	}

	@Test
	public void shouldCreatePredicateWithInWhenPropertyIsAnObjectAndValueIsACollection() {
		Predicate predicate = binding.bind(QUser.user.firstname, Arrays.asList("dragon reborn", "shadowkiller"), OperationType.NO_OOP);
		assertPredicate(predicate, is(QUser.user.firstname.in(Arrays.asList("dragon reborn", "shadowkiller"))));
	}

	@Test
	public void testname() {
		assertThat(binding.bind(QUser.user.lastname, Collections.emptySet(), OperationType.NO_OOP), is(nullValue()));
	}

	@Test
	public void shouldCreatePredicateCorrectlyEq() {
		Predicate predicate = binding.bind(QUser.user.firstname, Collections.singleton("tam"), OperationType.EQ);
		assertPredicate(predicate, is(QUser.user.firstname.eq("tam")));
	}

	@Test
	public void shouldCreatePredicateCorrectlyNe() {
		Predicate predicate = binding.bind(QUser.user.firstname, Collections.singleton("tam"), OperationType.NE);
		assertPredicate(predicate, is(QUser.user.firstname.ne("tam")));
	}

	@Test
	public void shouldCreatePredicateCorrectlyContains() {
		Predicate predicate = binding.bind(QUser.user.nickNames, Collections.singleton("nick"), OperationType.CONTAINS);
		assertPredicate(predicate, is(QUser.user.nickNames.contains("nick")));
	}

	@Test
	public void shouldCreatePredicateCorrectlyLike() {
		Predicate predicate = binding.bind(QUser.user.firstname, Collections.singleton("nick"), OperationType.LIKE);
		assertPredicate(predicate, is(QUser.user.firstname.likeIgnoreCase("%nick%")));
	}

	@Test
	public void shouldCreatePredicateCorrectlyGt() {
		Predicate predicate = binding.bind(QUser.user.inceptionYear, Collections.singleton(20L), OperationType.GT);
		assertPredicate(predicate, is(QUser.user.inceptionYear.gt(20)));
	}

	@Test
	public void shouldCreatePredicateCorrectlyGoe() {
		Predicate predicate = binding.bind(QUser.user.inceptionYear, Collections.singleton(20L), OperationType.GOE);
		assertPredicate(predicate, is(QUser.user.inceptionYear.goe(20)));
	}

	@Test
	public void shouldCreatePredicateCorrectlyLt() {
		Predicate predicate = binding.bind(QUser.user.inceptionYear, Collections.singleton(20L), OperationType.LT);
		assertPredicate(predicate, is(QUser.user.inceptionYear.lt(20)));
	}

	@Test
	public void shouldCreatePredicateCorrectlyLoe() {
		Predicate predicate = binding.bind(QUser.user.inceptionYear, Collections.singleton(20L), OperationType.LOE);
		assertPredicate(predicate, is(QUser.user.inceptionYear.loe(20)));
	}

	@Test
	public void shouldCreatePredicateCorrectlyIn() {
		Predicate predicate = binding.bind(QUser.user.inceptionYear, Arrays.asList(1L, 2L), OperationType.IN);
		assertPredicate(predicate, is(QUser.user.inceptionYear.in(Arrays.asList(1L, 2L))));
	}

	@Test
	public void shouldCreatePredicateCorrectlyNotIn() {
		Predicate predicate = binding.bind(QUser.user.inceptionYear, Arrays.asList(1L, 2L), OperationType.NOT_IN);
		assertPredicate(predicate, is(QUser.user.inceptionYear.notIn(Arrays.asList(1L, 2L))));
	}

	@Test
	public void shouldCreatePredicateCorrectlyNotIn2() {
		Predicate predicate = binding.bind(QUser.user.inceptionYear, Arrays.asList(1L, 2L), OperationType.NOT_IN2);
		assertPredicate(predicate, is(QUser.user.inceptionYear.in(Arrays.asList(1L, 2L)).not()));
	}

	@Test
	public void shouldCreatePredicateCorrectlyIsNull() {
		Predicate predicate = binding.bind(QUser.user.inceptionYear, null, OperationType.IS_NULL);
		assertPredicate(predicate, is(QUser.user.inceptionYear.isNull()));
	}

	@Test
	public void shouldCreatePredicateCorrectlyIsNotNull() {
		Predicate predicate = binding.bind(QUser.user.inceptionYear, null, OperationType.NOT_NULL);
		assertPredicate(predicate, is(QUser.user.inceptionYear.isNotNull()));
	}

	/*
	 * just to satisfy generic type boundaries o_O
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void assertPredicate(Predicate predicate, Matcher<? extends Expression> matcher) {
		assertThat((Expression) predicate, Is.<Expression>is((Matcher<Expression>) matcher));
	}
}
