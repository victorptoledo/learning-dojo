package dojo.secure;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/** A user of the API. Only the hash of the password is kept. */
@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles = new TreeSet<>();

    /** Wrong passwords in a row since the last good login. */
    private int failedLogins;

    /** Logins are refused until this instant; null when the account is not locked. */
    private Instant lockedUntil;

    protected AppUser() {
    }

    public AppUser(String username, String passwordHash, Set<String> roles) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles.addAll(roles);
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Collection<String> roles) {
        this.roles.clear();
        this.roles.addAll(roles);
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /** Counts a wrong password; returns true when this one locks the account. */
    public boolean failedLogin(int attempts, Instant until) {
        failedLogins++;
        if (failedLogins >= attempts) {
            failedLogins = 0;
            lockedUntil = until;
            return true;
        }
        return false;
    }

    public void goodLogin() {
        failedLogins = 0;
        lockedUntil = null;
    }

    public int getFailedLogins() {
        return failedLogins;
    }
}
