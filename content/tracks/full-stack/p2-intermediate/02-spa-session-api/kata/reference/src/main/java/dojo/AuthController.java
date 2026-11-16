package dojo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final SecurityContextHolderStrategy contexts = SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository sessions = new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager, Profiles profiles) {
        this.authenticationManager = authenticationManager;
        this.profiles = profiles;
    }

    @GetMapping("/api/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/api/login")
    ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest login,
                                              HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(login.username(), login.password()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "bad credentials"));
        }
        SecurityContext context = contexts.createEmptyContext();
        context.setAuthentication(authentication);
        contexts.setContext(context);
        sessions.saveContext(context, request, response);
        return ResponseEntity.ok(Map.of("username", authentication.getName()));
    }

    @GetMapping("/api/me")
    Map<String, Object> me(Authentication authentication) {
        return view(authentication);
    }

    @PutMapping("/api/me")
    ResponseEntity<Map<String, Object>> rename(@RequestBody Rename rename, Authentication authentication) {
        if (rename.displayName() == null || rename.displayName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "displayName must not be blank"));
        }
        profiles.rename(authentication.getName(), rename.displayName().strip());
        return ResponseEntity.ok(view(authentication));
    }

    @PostMapping("/api/logout")
    ResponseEntity<Void> logout(Authentication authentication,
                                HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> view(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(role -> role.substring("ROLE_".length()))
                .sorted()
                .toList();
        return Map.of(
                "username", authentication.getName(),
                "displayName", profiles.displayName(authentication.getName()),
                "roles", roles);
    }
}
