package dojo;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {
    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }
}
