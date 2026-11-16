package dojo;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/csrf, POST /api/login, GET and PUT /api/me, POST /api/logout (see README.md).
 */
@RestController
public class AuthController {
    public record LoginRequest(String username, String password) {}

    public record Rename(String displayName) {}

    private final AuthenticationManager authenticationManager;
    private final Profiles profiles;

    public AuthController(AuthenticationManager authenticationManager, Profiles profiles) {
        this.authenticationManager = authenticationManager;
        this.profiles = profiles;
    }

    // todo: the five endpoints
}
