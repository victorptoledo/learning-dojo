package dojo;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class CheckoutController {
    private final Rollout rollout;
    private final CheckoutSaga saga;
    private final LegacyCheckout legacy;
    private final OrderRepository orders;

    public CheckoutController(Rollout rollout, CheckoutSaga saga, LegacyCheckout legacy, OrderRepository orders) {
        this.rollout = rollout;
        this.saga = saga;
        this.legacy = legacy;
        this.orders = orders;
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResult> checkout(@RequestHeader("X-User") String user,
                                                   @Valid @RequestBody CheckoutRequest request) {
        CheckoutResult result = rollout.enabled(FlagStore.SAGA_CHECKOUT, user)
                ? saga.start(request)
                : legacy.checkout(request);
        HttpStatus status = result.status() == OrderStatus.CONFIRMED ? HttpStatus.CREATED : HttpStatus.UNPROCESSABLE_CONTENT;
        return ResponseEntity.status(status).body(result);
    }

    /** Reads the new column, and the old one for the rows the backfill has not reached yet. */
    @GetMapping("/orders/{id}")
    public OrderView order(@PathVariable long id) {
        ShopOrder order = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String customer = order.getCustomer() != null ? order.getCustomer() : order.getCustomerName();
        return new OrderView(order.getId(), customer, order.getStatus(), order.getReason());
    }
}
