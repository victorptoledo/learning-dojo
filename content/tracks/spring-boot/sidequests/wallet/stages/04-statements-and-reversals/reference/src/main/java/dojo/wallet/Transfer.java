package dojo.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** Money moved from one account to another. */
@Entity
public class Transfer {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "from_account")
    private long from;

    @Column(name = "to_account")
    private long to;

    private long amountCents;

    /** The transfer this one undoes; unique, so the database refuses a second reversal. */
    @Column(unique = true)
    private Long reverses;

    protected Transfer() {
    }

    public Transfer(long from, long to, long amountCents) {
        this(from, to, amountCents, null);
    }

    public Transfer(long from, long to, long amountCents, Long reverses) {
        this.from = from;
        this.to = to;
        this.amountCents = amountCents;
        this.reverses = reverses;
    }

    public Long getId() {
        return id;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public Long getReverses() {
        return reverses;
    }
}
