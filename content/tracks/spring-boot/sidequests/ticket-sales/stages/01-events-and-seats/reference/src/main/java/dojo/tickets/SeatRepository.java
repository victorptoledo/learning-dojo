package dojo.tickets;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByEventIdOrderByRowIndexAscNumberAsc(long eventId);

    long countByEventIdAndStatus(long eventId, SeatStatus status);
}
