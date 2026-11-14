package dojo;

import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrackerService {
    private static final Set<String> PATCHABLE = Set.of("title", "status", "assignee");
    private final ProjectRepository projects;
    private final IssueRepository issues;

    public TrackerService(ProjectRepository projects, IssueRepository issues) {
        this.projects = projects;
        this.issues = issues;
    }

    @Transactional
    public void createProject(NewProject request) {
        if (projects.existsByKey(request.key())) {
            throw TrackerException.duplicateProject(request.key());
        }
        projects.save(new Project(request.key(), request.name().strip()));
    }

    @Transactional
    public IssueView createIssue(String projectKey, NewIssue request) {
        Project project = project(projectKey);
        int number = issues.maxNumber(project) + 1;
        String assignee = request.assignee() == null || request.assignee().isBlank() ? null : request.assignee().strip();
        return IssueView.of(issues.saveAndFlush(new Issue(project, number, request.title().strip(), assignee)));
    }

    @Transactional(readOnly = true)
    public IssueView get(String projectKey, int number) {
        return IssueView.of(issue(projectKey, number));
    }

    @Transactional(readOnly = true)
    public IssuePage list(String projectKey, IssueStatus status, String assignee, int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw TrackerException.badRequest("page must be 0 or more and size 1 to 50");
        }
        Page<Issue> found = issues.search(project(projectKey), status, assignee,
                PageRequest.of(page, size, Sort.by("number")));
        return new IssuePage(found.map(IssueView::of).getContent(), page, size, found.getTotalElements());
    }

    /** Applies a JSON merge patch if {@code expectedVersion} is the issue's current version. */
    @Transactional
    public IssueView patch(String projectKey, int number, Long expectedVersion, Map<String, Object> patch) {
        Issue issue = issue(projectKey, number);
        if (expectedVersion == null) {
            throw TrackerException.ifMatchRequired();
        }
        if (expectedVersion != issue.getVersion()) {
            throw TrackerException.stale(issue.getVersion());
        }
        for (var entry : patch.entrySet()) {
            if (!PATCHABLE.contains(entry.getKey())) {
                throw TrackerException.badRequest("cannot change " + entry.getKey());
            }
        }
        if (patch.containsKey("title")) {
            if (!(patch.get("title") instanceof String title) || title.isBlank()) {
                throw TrackerException.badRequest("title must not be blank");
            }
            issue.setTitle(title.strip());
        }
        if (patch.containsKey("status")) {
            try {
                issue.setStatus(IssueStatus.valueOf(String.valueOf(patch.get("status"))));
            } catch (IllegalArgumentException e) {
                throw TrackerException.badRequest("unknown status " + patch.get("status"));
            }
        }
        if (patch.containsKey("assignee")) {
            Object assignee = patch.get("assignee");
            issue.setAssignee(assignee == null ? null : assignee.toString().strip());
        }
        return IssueView.of(issues.saveAndFlush(issue));
    }

    private Project project(String key) {
        return projects.findByKey(key).orElseThrow(() -> TrackerException.projectNotFound(key));
    }

    private Issue issue(String key, int number) {
        return issues.findByProjectAndNumber(project(key), number)
                .orElseThrow(() -> TrackerException.issueNotFound(key, number));
    }
}
