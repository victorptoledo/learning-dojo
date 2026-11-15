package dojo;

/** Takes payments at checkout through whatever gateway it is given. */
public class CheckoutService {
    private final PaymentGateway gateway;

    public CheckoutService(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public Receipt pay(Payment payment) {
        if (payment.cents() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        return gateway.charge(payment);
    }
}
