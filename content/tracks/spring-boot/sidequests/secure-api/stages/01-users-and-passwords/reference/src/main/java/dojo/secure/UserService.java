package dojo.secure;

import java.util.Locale;
import java.util.Set;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Signs users up and gives Spring Security their details. */
@Service
public class UserService implements UserDetailsService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public AppUser signup(SignupRequest request) {
        checkPassword(request.username(), request.password());
        if (users.existsByUsername(request.username())) {
            throw new ConflictException("username " + request.username() + " is taken");
        }
        // The unique constraint refuses the duplicates that race past the check.
        return users.saveAndFlush(new AppUser(request.username(), encoder.encode(request.password()), Set.of("USER")));
    }

    static void checkPassword(String username, String password) {
        if (password.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
            throw new FieldException("password", "must not contain the username");
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
                .build();
    }
}
