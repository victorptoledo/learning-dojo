package dojo;

/**
 * A question about customers, as data. The same spec is answered in memory by
 * {@link #isSatisfiedBy} and turned into SQL by {@link CustomerQueries#where}.
 */
public sealed interface CustomerSpec permits IsActive, InCountry, MinOrders, And, Or, Not {
    static CustomerSpec active() {
        return new IsActive();
    }

    static CustomerSpec inCountry(String country) {
        return new InCountry(country);
    }

    static CustomerSpec minOrders(int count) {
        return new MinOrders(count);
    }

    default CustomerSpec and(CustomerSpec other) {
        return new And(this, other);
    }

    default CustomerSpec or(CustomerSpec other) {
        return new Or(this, other);
    }

    default CustomerSpec not() {
        return new Not(this);
    }

    /** The in-memory interpreter. */
    default boolean isSatisfiedBy(Customer customer) {
        return switch (this) {
            case IsActive active -> customer.active();
            case InCountry(String country) -> customer.country().equals(country);
            case MinOrders(int count) -> customer.orders() >= count;
            case And(CustomerSpec left, CustomerSpec right) -> left.isSatisfiedBy(customer) && right.isSatisfiedBy(customer);
            case Or(CustomerSpec left, CustomerSpec right) -> left.isSatisfiedBy(customer) || right.isSatisfiedBy(customer);
            case Not(CustomerSpec spec) -> !spec.isSatisfiedBy(customer);
        };
    }
}
