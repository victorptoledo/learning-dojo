package dojo;

/**
 * What checkout needs from a payment provider, in our own terms. Throws
 * {@link PaymentDeclinedException} or {@link GatewayUnavailableException}.
 */
@FunctionalInterface
public interface PaymentGateway {
    Receipt charge(Payment payment);
}
