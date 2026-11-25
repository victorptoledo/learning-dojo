package dojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;

/** How many units of a currency one euro buys, and when that was last set. */
@Entity
public class Rate {
    @Id
    private String code;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal perEuro;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Rate() {
    }

    public Rate(String code, BigDecimal perEuro, Instant updatedAt) {
        this.code = code;
        this.perEuro = perEuro;
        this.updatedAt = updatedAt;
    }

    public void change(BigDecimal perEuro, Instant at) {
        this.perEuro = perEuro;
        this.updatedAt = at;
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getPerEuro() {
        return perEuro;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
