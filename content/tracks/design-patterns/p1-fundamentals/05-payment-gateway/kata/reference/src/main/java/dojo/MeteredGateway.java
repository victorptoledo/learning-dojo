package dojo;

/** Decorator: counts the calls that succeed and the calls that fail, and changes nothing else. */
public final class MeteredGateway implements PaymentGateway {
    private final PaymentGateway inner;
    private int successes;
    private int failures;

    public MeteredGateway(PaymentGateway inner) {
        this.inner = inner;
    }

    @Override
    public Receipt charge(Payment payment) {
        try {
            Receipt receipt = inner.charge(payment);
            successes++;
            return receipt;
        } catch (RuntimeException e) {
            failures++;
            throw e;
        }
    }

    public int successes() {
        return successes;
    }

    public int failures() {
        return failures;
    }
}
