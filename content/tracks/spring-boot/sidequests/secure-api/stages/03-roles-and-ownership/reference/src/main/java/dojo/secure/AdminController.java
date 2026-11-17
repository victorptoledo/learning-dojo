package dojo.secure;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserRepository users;

    public AdminController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<UserView> users() {
        return users.findAllByOrderByUsername().stream().map(UserView::of).toList();
    }

    @PutMapping("/users/{username}/roles")
    @Transactional
    public UserView roles(@PathVariable String username, @Valid @RequestBody RolesRequest request, Authentication me) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("user " + username + " not found"));
        if (username.equals(me.getName()) && !request.roles().contains("ADMIN")) {
            throw new ConflictException("you cannot remove your own admin role");
        }
        user.setRoles(request.roles());
        return UserView.of(user);
    }
}
