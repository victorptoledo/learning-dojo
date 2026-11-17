package dojo.wallet;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** One change of one balance, written in the same transaction as the change. */
@Entity
@Table(indexes = @Index(columnList = "account_id, at"))
public class Entry {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    private EntryType type;

    private long amountCents;

    private long balanceAfterCents;

    private Long transferId;

    private Instant at;

    protected Entry() {
    }

    public Entry(Account account, EntryType type, long amountCents, Long transferId, Instant at) {
        this.account = account;
        this.type = type;
        this.amountCents = amountCents;
        this.balanceAfterCents = account.getBalanceCents();
        this.transferId = transferId;
        this.at = at;
    }

    public Long getId() {
        return id;
    }

    public EntryType getType() {
        return type;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public long getBalanceAfterCents() {
        return balanceAfterCents;
    }

    public Long getTransferId() {
        return transferId;
    }

    public Instant getAt() {
        return at;
    }
}
