package dojo;

/** What POST /payments answers: the payment created (or replayed), and whether it is a replay. */
public record PaymentOutcome(PaymentView payment, boolean replayed) {}
