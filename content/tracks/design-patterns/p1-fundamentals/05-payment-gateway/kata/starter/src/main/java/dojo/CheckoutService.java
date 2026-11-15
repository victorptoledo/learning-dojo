package dojo;

/**
 * Takes payments at checkout. It speaks the SDK's language directly, and retries and
 * counting are tangled into the same method, so none of it can be reused or reordered.
 */
public class CheckoutService {
    private final LegacyPayClient client;
    private int successes;
    private int failures;

    public CheckoutService(LegacyPayClient client) {
        this.client = client;
    }

    public Receipt pay(Payment payment) {
        if (payment.cents() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        String amount = payment.cents() / 100 + "." + String.format("%02d", payment.cents() % 100);
        for (int attempt = 1; attempt <= 3; attempt++) {
            int status = client.submit(payment.account(), amount, payment.currency());
            if (status == LegacyPayClient.OK) {
                successes++;
                return new Receipt(client.lastReference(), payment.cents());
            }
            if (status == LegacyPayClient.DECLINED) {
                failures++;
                throw new PaymentDeclinedException("declined: " + payment.account());
            }
        }
        failures++;
        throw new GatewayUnavailableException("provider busy");
    }

    public int successes() {
        return successes;
    }

    public int failures() {
        return failures;
    }
}
