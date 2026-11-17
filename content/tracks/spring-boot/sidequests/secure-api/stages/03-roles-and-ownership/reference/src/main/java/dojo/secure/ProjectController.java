package dojo.secure;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projects;

    public ProjectController(ProjectService projects) {
        this.projects = projects;
    }

    @PostMapping
    public ResponseEntity<ProjectView> create(@Valid @RequestBody ProjectRequest request, Authentication user,
            UriComponentsBuilder uri) {
        Project project = projects.create(request, user);
        return ResponseEntity.created(uri.path("/api/projects/{id}").build(project.getId()))
                .body(ProjectView.of(project));
    }

    @GetMapping
    public List<ProjectView> mine(Authentication user) {
        return projects.visibleTo(user).stream().map(ProjectView::of).toList();
    }

    @GetMapping("/{id}")
    public ProjectView get(@PathVariable long id, Authentication user) {
        return ProjectView.of(projects.get(id, user));
    }

    @PutMapping("/{id}/members/{username}")
    public ProjectView addMember(@PathVariable long id, @PathVariable String username, Authentication user) {
        return ProjectView.of(projects.addMember(id, username, user));
    }

    @DeleteMapping("/{id}/members/{username}")
    public ProjectView removeMember(@PathVariable long id, @PathVariable String username, Authentication user) {
        return ProjectView.of(projects.removeMember(id, username, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id, Authentication user) {
        projects.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
