package dojo.tickets;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface HoldRepository extends JpaRepository<Hold, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hold h where h.id = :id")
    Optional<Hold> lock(long id);

    /** Only the event id: an entity read before its lock would stay stale in the persistence context. */
    @Query("select h.eventId from Hold h where h.id = :id and h.buyer = :buyer")
    Optional<Long> eventOf(long id, String buyer);

    @Query("""
            select count(h) from Hold h
            where h.eventId = :eventId and h.status = dojo.tickets.HoldStatus.HELD and h.expiresAt > :now""")
    long countActive(long eventId, Instant now);

    @Modifying(flushAutomatically = true)
    @Query("update Hold h set h.status = dojo.tickets.HoldStatus.RELEASED "
            + "where h.eventId = :eventId and h.status = dojo.tickets.HoldStatus.HELD")
    int releaseAll(long eventId);
}
