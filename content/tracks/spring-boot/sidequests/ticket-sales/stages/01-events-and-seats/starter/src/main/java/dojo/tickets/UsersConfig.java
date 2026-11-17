package dojo.tickets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Demo users kept in memory, with plain-text passwords ({noop}) so the load tests stay fast.
 * Never do this in a real application: store BCrypt hashes in the database, as in the
 * Secure API sidequest.
 */
@Configuration
public class UsersConfig {
    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pass").roles("USER").build(),
                User.withUsername("linus").password("{noop}linus-pass").roles("USER").build(),
                User.withUsername("grace").password("{noop}grace-pass").roles("USER").build(),
                User.withUsername("boss").password("{noop}boss-pass").roles("ORGANIZER").build());
    }
}
