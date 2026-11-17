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

    protected Transfer() {
    }

    public Transfer(long from, long to, long amountCents) {
        this.from = from;
        this.to = to;
        this.amountCents = amountCents;
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
}
