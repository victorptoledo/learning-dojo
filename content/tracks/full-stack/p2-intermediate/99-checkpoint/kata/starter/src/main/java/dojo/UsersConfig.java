package dojo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/** Three users: ada and linus (USER) and root (USER and ADMIN). */
@Configuration
public class UsersConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("ada").password(encoder.encode("ada-pass")).roles("USER").build(),
                User.withUsername("linus").password(encoder.encode("linus-pass")).roles("USER").build(),
                User.withUsername("root").password(encoder.encode("root-pass")).roles("USER", "ADMIN").build());
    }
}
