package dojo.secure;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Signs users up, changes passwords and gives Spring Security their details. */
@Service
public class UserService implements UserDetailsService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Audit audit;
    private final Clock clock;

    public UserService(UserRepository users, PasswordEncoder encoder, Audit audit, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
        this.clock = clock;
    }

    public AppUser signup(SignupRequest request) {
        checkPassword("password", request.username(), request.password());
        if (users.existsByUsername(request.username())) {
            throw new ConflictException("username " + request.username() + " is taken");
        }
        // The unique constraint refuses the duplicates that race past the check.
        return users.saveAndFlush(new AppUser(request.username(), encoder.encode(request.password()), Set.of("USER")));
    }

    @Transactional
    public void changePassword(String username, PasswordChange change) {
        AppUser user = users.findByUsername(username).orElseThrow();
        if (!encoder.matches(change.currentPassword(), user.getPasswordHash())) {
            throw new FieldException("currentPassword", "is wrong");
        }
        checkPassword("newPassword", username, change.newPassword());
        user.setPasswordHash(encoder.encode(change.newPassword()));
        audit.record(AuditEvent.Type.PASSWORD_CHANGED, username);
    }

    static void checkPassword(String field, String username, String password) {
        if (password.length() < 12 || password.length() > 72) {
            throw new FieldException(field, "size must be between 12 and 72");
        }
        if (password.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
            throw new FieldException(field, "must not contain the username");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("unknown user"));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRoles().toArray(String[]::new))
                .accountLocked(user.isLockedAt(Instant.now(clock)))
                .build();
    }
}
