package dojo;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    Optional<Issue> findByProjectAndNumber(Project project, int number);

    @Query("select coalesce(max(i.number), 0) from Issue i where i.project = :project")
    int maxNumber(@Param("project") Project project);

    @Query("""
            select i from Issue i
            where i.project = :project
              and (:status is null or i.status = :status)
              and (:assignee is null or i.assignee = :assignee)
            """)
    Page<Issue> search(@Param("project") Project project, @Param("status") IssueStatus status,
                       @Param("assignee") String assignee, Pageable pageable);
}
