package dojo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    // the pending events (not published, not dead) in the order they were written
}
