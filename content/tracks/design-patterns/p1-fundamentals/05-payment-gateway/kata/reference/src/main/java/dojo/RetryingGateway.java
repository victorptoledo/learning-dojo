package dojo;

/** Decorator: tries again when the provider is unavailable, never when it declines. */
public final class RetryingGateway implements PaymentGateway {
    private final PaymentGateway inner;
    private final int maxAttempts;

    public RetryingGateway(PaymentGateway inner, int maxAttempts) {
        if (maxAttempts < 1) throw new IllegalArgumentException("at least one attempt");
        this.inner = inner;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public Receipt charge(Payment payment) {
        GatewayUnavailableException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return inner.charge(payment);
            } catch (GatewayUnavailableException e) {
                last = e;
            }
        }
        throw last;
    }
}
