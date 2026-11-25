package dojo;

import org.springframework.web.bind.annotation.RestController;

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
}
