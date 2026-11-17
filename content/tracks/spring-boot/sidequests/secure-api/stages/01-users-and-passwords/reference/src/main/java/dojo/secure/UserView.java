package dojo.secure;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** A user as the API shows it: never the password. */
public record UserView(String username, List<String> roles) {
    static UserView of(AppUser user) {
        return new UserView(user.getUsername(), List.copyOf(user.getRoles()));
    }

    static UserView of(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .sorted()
                .toList();
        return new UserView(authentication.getName(), roles);
    }
}
