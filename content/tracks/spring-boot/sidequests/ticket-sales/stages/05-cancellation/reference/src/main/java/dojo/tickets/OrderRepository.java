package dojo.tickets;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<TicketOrder, Long> {
    Optional<TicketOrder> findByBuyerAndIdempotencyKey(String buyer, String idempotencyKey);

    /** How many orders of the event have that status, and their total, in one query. */
    @Query("""
            select new dojo.tickets.OrderTotals(count(o), coalesce(sum(o.totalCents), 0))
            from TicketOrder o where o.eventId = :eventId and o.status = :status""")
    OrderTotals totals(long eventId, OrderStatus status);

    /** The seats of the paid orders of the event. */
    @Query("""
            select count(s) from TicketOrder o join o.seats s
            where o.eventId = :eventId and o.status = dojo.tickets.OrderStatus.PAID""")
    long countSoldSeats(long eventId);

    @Modifying(flushAutomatically = true)
    @Query("update TicketOrder o set o.status = dojo.tickets.OrderStatus.REFUNDED "
            + "where o.eventId = :eventId and o.status = dojo.tickets.OrderStatus.PAID")
    int refundAll(long eventId);
}
