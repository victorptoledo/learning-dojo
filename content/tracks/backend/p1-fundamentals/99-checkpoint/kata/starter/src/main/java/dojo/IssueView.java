package dojo;

public record IssueView(String key, int number, String title, IssueStatus status, String assignee, long version) {
    public static IssueView of(Issue issue) {
        return new IssueView(issue.getProject().getKey() + "-" + issue.getNumber(), issue.getNumber(),
                issue.getTitle(), issue.getStatus(), issue.getAssignee(), issue.getVersion());
    }
}
