package dojo.secure;

import java.time.Clock;
import java.time.Instant;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Counts wrong passwords from the events Spring Security publishes, and locks accounts. */
@Service
public class LoginGuard {
    private final UserRepository users;
    private final Audit audit;
    private final SecureProperties properties;
    private final Clock clock;

    public LoginGuard(UserRepository users, Audit audit, SecureProperties properties, Clock clock) {
        this.users = users;
        this.audit = audit;
        this.properties = properties;
        this.clock = clock;
    }

    @EventListener
    @Transactional
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        users.findByUsername(event.getAuthentication().getName()).ifPresent(user -> {
            audit.record(AuditEvent.Type.LOGIN_FAILURE, user.getUsername());
            Instant until = Instant.now(clock).plus(properties.lockout().duration());
            if (user.failedLogin(properties.lockout().attempts(), until)) {
                audit.record(AuditEvent.Type.ACCOUNT_LOCKED, user.getUsername());
            }
        });
    }

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        users.findByUsername(event.getAuthentication().getName())
                .filter(user -> user.getFailedLogins() > 0)
                .ifPresent(AppUser::goodLogin);
    }
}
