package dojo.secure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("select distinct p from Project p left join p.members m where p.owner = :username or m = :username order by p.id")
    List<Project> visibleTo(String username);
}
