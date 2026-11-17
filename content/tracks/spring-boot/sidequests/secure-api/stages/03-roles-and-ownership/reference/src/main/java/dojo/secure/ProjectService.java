package dojo.secure;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every rule about who may see or change a project lives here. */
@Service
public class ProjectService {
    private final ProjectRepository projects;
    private final UserRepository users;

    public ProjectService(ProjectRepository projects, UserRepository users) {
        this.projects = projects;
        this.users = users;
    }

    @Transactional
    public Project create(ProjectRequest request, Authentication user) {
        return projects.save(new Project(request.name().strip(), user.getName()));
    }

    @Transactional(readOnly = true)
    public List<Project> visibleTo(Authentication user) {
        return projects.visibleTo(user.getName());
    }

    /** The project if the user may see it; a stranger gets the same 404 as for a missing one. */
    @Transactional(readOnly = true)
    public Project get(long id, Authentication user) {
        Project project = projects.findById(id).orElseThrow(() -> new NotFoundException("project " + id + " not found"));
        boolean sees = isAdmin(user) || project.getOwner().equals(user.getName())
                || project.getMembers().contains(user.getName());
        if (!sees) {
            throw new NotFoundException("project " + id + " not found");
        }
        return project;
    }

    /** The project if the user may change it: members see it, but get 403. */
    private Project editable(long id, Authentication user) {
        Project project = get(id, user);
        if (!isAdmin(user) && !project.getOwner().equals(user.getName())) {
            throw new ForbiddenException("only the owner changes project " + id);
        }
        return project;
    }

    @Transactional
    public Project addMember(long id, String username, Authentication user) {
        Project project = editable(id, user);
        if (!users.existsByUsername(username)) {
            throw new NotFoundException("user " + username + " not found");
        }
        project.getMembers().add(username);
        return project;
    }

    @Transactional
    public Project removeMember(long id, String username, Authentication user) {
        Project project = editable(id, user);
        project.getMembers().remove(username);
        return project;
    }

    @Transactional
    public void delete(long id, Authentication user) {
        projects.delete(editable(id, user));
    }

    static boolean isAdmin(Authentication user) {
        return user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
