package org.springframework.data.querydsl.binding;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.CollectionPathBase;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.core.types.dsl.StringExpression;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum OperationType {
	NO_OOP("") {
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		protected Predicate getPredicateImpl(Path<?> path, Collection<?> value) {
			if (path instanceof CollectionPathBase) {

				BooleanBuilder builder = new BooleanBuilder();

				for (Object element : value) {
					builder.and(((CollectionPathBase) path).contains(element));
				}

				return builder.getValue();
			}

			if (path instanceof SimpleExpression) {

				if (value.size() > 1) {
					return ((SimpleExpression) path).in(value);
				}

				return ((SimpleExpression) path).eq(value.iterator().next());
			}

			throw new IllegalArgumentException(
					String.format("Cannot create predicate for path '%s' with type '%s'.", path, path.getMetadata().getPathType()));
		}
	},
	NE("ne") {
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		protected Predicate getPredicateImpl(Path<?> path, Collection<?> value) {
			if (path instanceof SimpleExpression && value.size() == 1) {
				return ((SimpleExpression) path).ne(value.iterator().next());
			}
			throw new IllegalArgumentException("Invalid operation usage");
		}
	},
	EQ("eq") {
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		protected Predicate getPredicateImpl(Path<?> path, Collection<?> value) {
			if (path instanceof SimpleExpression && value.size() == 1) {
				return ((SimpleExpression) path).eq(value.iterator().next());
			}
			throw new IllegalArgumentException("Invalid operation usage");
		}
	},
	CONTAINS("contains") {
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		protected Predicate getPredicateImpl(Path<?> path, Collection<?> value) {
			if (path instanceof CollectionPathBase) {

				BooleanBuilder builder = new BooleanBuilder();

				for (Object element : value) {
					builder.and(((CollectionPathBase) path).contains(element));
				}

				return builder.getValue();
			}
			throw new IllegalArgumentException("Invalid operation usage");
		}
	},
	LIKE("like") {
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		protected Predicate getPredicateImpl(Path<?> path, Collection<?> value) {
			if (path instanceof StringExpression && value.size() == 1) {
				return ((StringExpression) path).likeIgnoreCase(String.valueOf(value.iterator().next()));
			}
			throw new IllegalArgumentException("Invalid operation usage");
		}
	},
	GT("gt") {
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		protected Predicate getPredicateImpl(Path<?> path, Collection<?> value) {
			if (path instanceof NumberExpression && value.size() == 1) {
				return ((NumberExpression) path).gt((Number) value.iterator().next());
			}
			throw new IllegalArgumentException("Invalid operation usage");
		}
	},
	GOE("goe") {
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		protected Predicate getPredicateImpl(Path<?> path, Collection<?> value) {
			if (path instanceof NumberExpression && value.size() == 1) {
				return ((NumberExpression) path).goe((Number) value.iterator().next());
			}
			throw new IllegalArgumentException("Invalid operation usage");
		}
	},
	LT("lt") {
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		protected Predicate getPredicateImpl(Path<?> path, Collection<?> value) {
			if (path instanceof NumberExpression && value.size() == 1) {
				return ((NumberExpression) path).lt((Number) value.iterator().next());
			}
			throw new IllegalArgumentException("Invalid operation usage");
		}
	},
	LOE("loe") {
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		protected Predicate getPredicateImpl(Path<?> path, Collection<?> value) {
			if (path instanceof NumberExpression && value.size() == 1) {
				return ((NumberExpression) path).loe((Number) value.iterator().next());
			}
			throw new IllegalArgumentException("Invalid operation usage");
		}
	},
	NOT_IN("notIn") {
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		protected Predicate getPredicateImpl(Path<?> path, Collection<?> value) {
			if (path instanceof SimpleExpression) {
				return ((SimpleExpression) path).notIn(value);
			}
			throw new IllegalArgumentException("Invalid operation usage");
		}
	},
	NOT_IN2("notIn2") {
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		protected Predicate getPredicateImpl(Path<?> path, Collection<?> value) {
			if (path instanceof SimpleExpression) {
				return ((SimpleExpression) path).in(value).not();
			}
			throw new IllegalArgumentException("Invalid operation usage");
		}
	},
	IN("in") {
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		protected Predicate getPredicateImpl(Path<?> path, Collection<?> value) {
			if (path instanceof SimpleExpression) {
				return ((SimpleExpression) path).in(value);
			}
			throw new IllegalArgumentException("Invalid operation usage");
		}
	},
	IS_NULL("isNull") {
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		public Predicate getPredicate(Path<?> path, Collection<?> value) {
			Assert.notNull(path, "Path must not be null!");
			if (path instanceof SimpleExpression) {
				return ((SimpleExpression) path).isNull();
			}
			throw new IllegalArgumentException("Invalid operation usage");
		}
	},
	NOT_NULL("notNull") {
		@Override
		@SuppressWarnings({"unchecked", "rawtypes"})
		public Predicate getPredicate(Path<?> path, Collection<?> value) {
			Assert.notNull(path, "Path must not be null!");
			if (path instanceof SimpleExpression) {
				return ((SimpleExpression) path).isNotNull();
			}
			throw new IllegalArgumentException("Invalid operation usage");
		}
	};

	private final String value;

	private static final Map<String, OperationType> VALUES;

	static {
		Map<String, OperationType> m = new HashMap<String, OperationType>();
		for (OperationType o : values()) {
			m.put(o.getValue().toLowerCase(), o);
		}
		VALUES = Collections.unmodifiableMap(m);
	}

	OperationType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public Predicate getPredicate(Path<?> path, Collection<? extends Object> value) {
		Assert.notNull(path, "Path must not be null!");
		Assert.notNull(value, "Value must not be null!");

		if (value.isEmpty()) {
			return null;
		}

		return getPredicateImpl(path, value);
	}

	protected Predicate getPredicateImpl(Path<?> path, Collection<? extends Object> value) {
		throw new IllegalArgumentException(
				String.format("Cannot create predicate for path '%s' with type '%s' and operation %s",
						path, path.getMetadata().getPathType(), this.getValue()));
	}

	public static OperationType value(String s) {
		if (StringUtils.isEmpty(s) || !VALUES.containsKey(s.toLowerCase())) {
			throw new IllegalStateException("Unknown operationType requested: " + s);
		}
		return VALUES.get(s.toLowerCase());
	}
}
