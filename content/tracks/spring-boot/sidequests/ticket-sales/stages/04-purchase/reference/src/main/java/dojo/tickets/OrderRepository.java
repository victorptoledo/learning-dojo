package dojo.tickets;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<TicketOrder, Long> {
    Optional<TicketOrder> findByBuyerAndIdempotencyKey(String buyer, String idempotencyKey);
}
