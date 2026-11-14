package dojo;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderView> place(@Valid @RequestBody OrderRequest request, UriComponentsBuilder uri) {
        OrderView order = orders.place(request);
        return ResponseEntity.created(uri.path("/orders/{id}").build(order.id())).body(order);
    }

    @GetMapping("/orders/{id}")
    public OrderView get(@PathVariable long id) {
        return orders.get(id);
    }

    @PostMapping("/orders/{id}/cancel")
    public OrderView cancel(@PathVariable long id) {
        return orders.cancel(id);
    }
}
