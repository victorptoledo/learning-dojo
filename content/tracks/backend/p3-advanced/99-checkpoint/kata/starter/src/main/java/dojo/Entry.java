package dojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** One line of a statement. Table {@code entries}: id, account_id, booked_on, description, amount. */
@Entity
@Table(name = "entries")
public class Entry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private long accountId;

    @Column(nullable = false)
    private LocalDate bookedOn;

    @Column(nullable = false)
    private String description;

    /** In cents; negative for money going out. */
    private long amount;

    protected Entry() {}

    public Entry(long accountId, LocalDate bookedOn, String description, long amount) {
        this.accountId = accountId;
        this.bookedOn = bookedOn;
        this.description = description;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public long getAccountId() {
        return accountId;
    }

    public LocalDate getBookedOn() {
        return bookedOn;
    }

    public String getDescription() {
        return description;
    }

    public long getAmount() {
        return amount;
    }
}
