package dojo.db;

import java.util.Set;

/** One condition of a query on a column. */
public sealed interface Predicate permits Predicate.Eq, Predicate.Range, Predicate.In {

    String column();

    /** {@code column == value} */
    record Eq(String column, long value) implements Predicate {
    }

    /** {@code from <= column < to}; empty when {@code from >= to} */
    record Range(String column, long from, long to) implements Predicate {
    }

    /** {@code column} is one of {@code values} */
    record In(String column, Set<Long> values) implements Predicate {
        public In {
            values = Set.copyOf(values);
        }
    }
}
