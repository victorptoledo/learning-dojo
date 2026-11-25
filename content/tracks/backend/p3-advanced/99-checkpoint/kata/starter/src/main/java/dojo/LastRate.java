package dojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** The last rate the rates service gave for a pair such as "USD:EUR", shared by every instance. */
@Entity
@Table(name = "last_rates")
public class LastRate {
    @Id
    private String pair;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    @Column(nullable = false)
    private Instant fetchedAt;

    protected LastRate() {}

    public LastRate(String pair, BigDecimal rate, Instant fetchedAt) {
        this.pair = pair;
        this.rate = rate;
        this.fetchedAt = fetchedAt;
    }

    public String getPair() {
        return pair;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }
}
