package dojo.wallet;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** An account of one owner in one currency. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"owner", "currency"}))
public class Account {
    @Id
    @GeneratedValue
    private Long id;

    private String owner;

    private String currency;

    private long balanceCents;

    protected Account() {
    }

    public Account(String owner, String currency) {
        this.owner = owner;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getCurrency() {
        return currency;
    }

    public long getBalanceCents() {
        return balanceCents;
    }

    public void credit(long cents) {
        balanceCents += cents;
    }

    /** Takes money out, or refuses when there is not enough. */
    public void debit(long cents) {
        if (cents > balanceCents) {
            throw new ConflictException("account " + id + " has " + balanceCents + " cents, needs " + cents);
        }
        balanceCents -= cents;
    }
}
