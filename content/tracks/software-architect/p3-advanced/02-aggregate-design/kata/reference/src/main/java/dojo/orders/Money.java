package dojo.orders;

/** A value object: an amount in one currency. */
public record Money(long cents, String currency) {
    public Money {
        if (cents < 0) {
            throw new IllegalArgumentException("negative amount " + cents);
        }
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("bad currency " + currency);
        }
    }

    public static Money zero(String currency) {
        return new Money(0, currency);
    }

    public Money plus(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("cannot add " + other.currency + " to " + currency);
        }
        return new Money(Math.addExact(cents, other.cents), currency);
    }

    public Money times(int factor) {
        return new Money(Math.multiplyExact(cents, factor), currency);
    }
}
