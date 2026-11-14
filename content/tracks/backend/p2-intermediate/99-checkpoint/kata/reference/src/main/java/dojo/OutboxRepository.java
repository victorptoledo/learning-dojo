package dojo;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    @Query("select e from OutboxEvent e where e.publishedAt is null and e.dead = false order by e.id")
    List<OutboxEvent> pending(Pageable page);
}
