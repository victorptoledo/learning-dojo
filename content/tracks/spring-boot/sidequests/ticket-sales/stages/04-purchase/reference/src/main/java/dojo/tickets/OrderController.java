package dojo.tickets;

import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class OrderController {
    private final PurchaseService purchases;

    public OrderController(PurchaseService purchases) {
        this.purchases = purchases;
    }

    @PostMapping("/holds/{holdId}/purchase")
    public ResponseEntity<OrderView> purchase(@PathVariable long holdId, @RequestHeader("Idempotency-Key") String key,
            Principal buyer, UriComponentsBuilder uri) {
        PurchaseService.Purchase purchase = purchases.purchase(holdId, key, buyer.getName());
        OrderView view = OrderView.of(purchase.order());
        return purchase.created()
                ? ResponseEntity.created(uri.path("/orders/{id}").build(view.id())).body(view)
                : ResponseEntity.ok(view);
    }

    @GetMapping("/orders/{id}")
    public OrderView get(@PathVariable long id, Principal buyer) {
        return OrderView.of(purchases.get(id, buyer.getName()));
    }
}
