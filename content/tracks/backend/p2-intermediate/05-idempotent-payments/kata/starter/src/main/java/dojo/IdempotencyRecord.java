package dojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

/**
 * What happened to one Idempotency-Key. The key is the primary key, so two requests cannot both
 * insert it. {@code done} is false while the first request is still running.
 *
 * <p>The id is assigned by us, not generated, so Spring Data would treat a new record as existing
 * and merge it (select, then update or insert), and two concurrent inserts would not collide.
 * {@link Persistable#isNew()} makes {@code save} a real insert for new records.
 */
@Entity
public class IdempotencyRecord implements Persistable<String> {
    @Transient
    private boolean isNew = true;

    @Id
    @Column(name = "idempotency_key", length = 100)
    private String key;

    @Column(nullable = false)
    private String fingerprint;

    private boolean done;

    private int status;

    private Long paymentId;

    private String detail;

    protected IdempotencyRecord() {}

    public IdempotencyRecord(String key, String fingerprint) {
        this.key = key;
        this.fingerprint = fingerprint;
    }

    public void finish(int status, Long paymentId, String detail) {
        this.done = true;
        this.status = status;
        this.paymentId = paymentId;
        this.detail = detail;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String getId() {
        return key;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        isNew = false;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public boolean isDone() {
        return done;
    }

    public int getStatus() {
        return status;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public String getDetail() {
        return detail;
    }
}
