package dojo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String account;

    private long amountCents;

    private String chargeId;

    protected Payment() {}

    public Payment(String account, long amountCents, String chargeId) {
        this.account = account;
        this.amountCents = amountCents;
        this.chargeId = chargeId;
    }

    public Long getId() {
        return id;
    }

    public String getAccount() {
        return account;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getChargeId() {
        return chargeId;
    }
}
