package dojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/** An issue is numbered inside its project: DOJO-1, DOJO-2, ... */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "number"}))
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status = IssueStatus.OPEN;

    private String assignee;

    /** Optimistic locking: Hibernate adds one on every update. It is also the ETag. */
    @Version
    private long version;

    protected Issue() {}

    public Issue(Project project, int number, String title, String assignee) {
        this.project = project;
        this.number = number;
        this.title = title;
        this.assignee = assignee;
    }

    public Project getProject() {
        return project;
    }

    public int getNumber() {
        return number;
    }

    public String getTitle() {
        return title;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public String getAssignee() {
        return assignee;
    }

    public long getVersion() {
        return version;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setStatus(IssueStatus status) {
        this.status = status;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
}
