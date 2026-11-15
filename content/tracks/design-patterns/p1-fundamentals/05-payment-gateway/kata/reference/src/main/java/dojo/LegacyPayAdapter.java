package dojo;

/** Adapter: makes the 2011 SDK look like a {@link PaymentGateway}. The only class that knows it. */
public final class LegacyPayAdapter implements PaymentGateway {
    private final LegacyPayClient client;

    public LegacyPayAdapter(LegacyPayClient client) {
        this.client = client;
    }

    @Override
    public Receipt charge(Payment payment) {
        String amount = "%d.%02d".formatted(payment.cents() / 100, payment.cents() % 100);
        int status = client.submit(payment.account(), amount, payment.currency());
        return switch (status) {
            case LegacyPayClient.OK -> new Receipt(client.lastReference(), payment.cents());
            case LegacyPayClient.DECLINED -> throw new PaymentDeclinedException("declined: " + payment.account());
            case LegacyPayClient.BUSY -> throw new GatewayUnavailableException("provider busy");
            default -> throw new GatewayUnavailableException("unknown provider status " + status);
        };
    }
}
