package dojo;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TrackerService {
    private final ProjectRepository projects;
    private final IssueRepository issues;

    public TrackerService(ProjectRepository projects, IssueRepository issues) {
        this.projects = projects;
        this.issues = issues;
    }

    public void createProject(NewProject request) {
        throw new UnsupportedOperationException("todo project " + request.key());
    }

    public IssueView createIssue(String projectKey, NewIssue request) {
        throw new UnsupportedOperationException("todo issue in " + projectKey);
    }

    public IssueView get(String projectKey, int number) {
        throw new UnsupportedOperationException("todo get " + projectKey + "-" + number);
    }

    public IssuePage list(String projectKey, IssueStatus status, String assignee, int page, int size) {
        throw new UnsupportedOperationException("todo list " + projectKey);
    }

    /** Applies a JSON merge patch if {@code expectedVersion} is the issue's current version. */
    public IssueView patch(String projectKey, int number, Long expectedVersion, Map<String, Object> patch) {
        throw new UnsupportedOperationException("todo patch " + projectKey + "-" + number);
    }
}
