package dojo.secure;

import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the first administrator from configuration when it is missing. */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final SecureProperties properties;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder, SecureProperties properties) {
        this.users = users;
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SecureProperties.Admin admin = properties.admin();
        if (!users.existsByUsername(admin.username())) {
            users.save(new AppUser(admin.username(), encoder.encode(admin.password()), Set.of("ADMIN", "USER")));
        }
    }
}
