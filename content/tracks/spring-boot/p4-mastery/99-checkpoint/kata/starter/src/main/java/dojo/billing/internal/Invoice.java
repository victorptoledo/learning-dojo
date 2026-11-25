package dojo.billing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;

/** Internal to billing: other modules never see this entity. */
@Entity
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String number;

    @Column(nullable = false)
    private String customer;

    private long amountCents;

    @Column(nullable = false)
    private LocalDate issuedOn;

    @Column(nullable = false)
    private LocalDate dueOn;

    protected Invoice() {
    }

    public Invoice(String customer, long amountCents, LocalDate issuedOn, LocalDate dueOn) {
        this.customer = customer;
        this.amountCents = amountCents;
        this.issuedOn = issuedOn;
        this.dueOn = dueOn;
    }

    public void assignNumber(String number) {
        this.number = number;
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getCustomer() {
        return customer;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }
}
