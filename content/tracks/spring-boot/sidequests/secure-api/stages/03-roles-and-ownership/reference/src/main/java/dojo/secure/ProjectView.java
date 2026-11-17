package dojo.secure;

import java.util.List;

/** A project as the API shows it. */
public record ProjectView(long id, String name, String owner, List<String> members) {
    static ProjectView of(Project project) {
        return new ProjectView(project.getId(), project.getName(), project.getOwner(),
                project.getMembers().stream().sorted().toList());
    }
}
