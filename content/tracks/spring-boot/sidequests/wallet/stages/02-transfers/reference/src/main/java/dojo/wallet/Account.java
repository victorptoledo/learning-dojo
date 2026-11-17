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

    private boolean closed;

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

    public boolean isClosed() {
        return closed;
    }

    public void ensureOpen() {
        if (closed) {
            throw new ConflictException("account " + id + " is closed");
        }
    }

    public void credit(long cents) {
        ensureOpen();
        balanceCents += cents;
    }

    /** Takes money out, or refuses when there is not enough. */
    public void debit(long cents) {
        ensureOpen();
        if (cents > balanceCents) {
            throw new ConflictException("account " + id + " has " + balanceCents + " cents, needs " + cents);
        }
        balanceCents -= cents;
    }

    public void close() {
        ensureOpen();
        if (balanceCents != 0) {
            throw new ConflictException("account " + id + " still has " + balanceCents + " cents");
        }
        closed = true;
    }
}
