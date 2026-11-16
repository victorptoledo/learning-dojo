package dojo;

import org.springframework.context.annotation.Configuration;

/**
 * The rules of README.md: a SecurityFilterChain bean (SPA CSRF, public login and csrf endpoints,
 * a JSON 401, no form login or HTTP Basic) and a CorsConfigurationSource bean for the SPA origin.
 */
@Configuration
public class SecurityConfig {
    // todo: the SecurityFilterChain and CorsConfigurationSource beans
}
