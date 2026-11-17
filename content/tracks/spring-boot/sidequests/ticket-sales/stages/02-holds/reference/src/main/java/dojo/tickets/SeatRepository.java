package dojo.tickets;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByEventIdOrderByRowIndexAscNumberAsc(long eventId);

    long countByEventIdAndStatus(long eventId, SeatStatus status);

    /** The seats, locked until the transaction ends, always in id order. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.event.id = :eventId and s.label in :labels order by s.id")
    List<Seat> lock(long eventId, Collection<String> labels);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.holdId = :holdId order by s.id")
    List<Seat> lockHeldBy(long holdId);
}
