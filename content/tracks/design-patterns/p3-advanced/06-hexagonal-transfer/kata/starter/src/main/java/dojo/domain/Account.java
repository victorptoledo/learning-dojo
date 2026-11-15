package dojo.domain;

/** A bank account. Anyone can change its balance at any time. */
public class Account {
    private final String id;
    private final String currency;
    private long balanceCents;

    public Account(String id, String currency, long balanceCents) {
        this.id = id;
        this.currency = currency;
        this.balanceCents = balanceCents;
    }

    public String getId() {
        return id;
    }

    public String getCurrency() {
        return currency;
    }

    public long getBalanceCents() {
        return balanceCents;
    }

    public void setBalanceCents(long balanceCents) {
        this.balanceCents = balanceCents;
    }
}
