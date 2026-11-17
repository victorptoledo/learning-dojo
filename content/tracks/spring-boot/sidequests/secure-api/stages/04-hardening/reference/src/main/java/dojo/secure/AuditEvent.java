package dojo.secure;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** One security event, kept for the administrators. */
@Entity
@Table(indexes = @Index(columnList = "username"))
public class AuditEvent {
    public enum Type {
        LOGIN_FAILURE, ACCOUNT_LOCKED, PASSWORD_CHANGED
    }

    @Id
    @GeneratedValue
    private Long id;

    private Type type;

    private String username;

    private Instant at;

    protected AuditEvent() {
    }

    public AuditEvent(Type type, String username, Instant at) {
        this.type = type;
        this.username = username;
        this.at = at;
    }

    public Type getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public Instant getAt() {
        return at;
    }
}
