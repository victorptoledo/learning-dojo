package dojo.tickets;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByEventIdOrderByRowIndexAscNumberAsc(long eventId);

    /** Seats nobody holds or bought at `now`, holds that ran out included. */
    @Query("""
            select count(s) from Seat s where s.event.id = :eventId and (s.status = dojo.tickets.SeatStatus.AVAILABLE
                or (s.status = dojo.tickets.SeatStatus.HELD and s.heldUntil <= :now))""")
    long countFree(long eventId, Instant now);

    /** The seats, locked until the transaction ends, always in id order. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.event.id = :eventId and s.label in :labels order by s.id")
    List<Seat> lock(long eventId, Collection<String> labels);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.holdId = :holdId order by s.id")
    List<Seat> lockHeldBy(long holdId);
}
