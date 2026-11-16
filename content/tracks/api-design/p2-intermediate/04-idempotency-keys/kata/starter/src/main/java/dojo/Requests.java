package dojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Request and response bodies. Written for you. Records compare by value. */
public final class Requests {
    private Requests() {}

    public record PaymentRequest(@NotBlank String account, @NotNull @Positive Long amountCents) {}

    public record RefundRequest(@NotBlank String paymentId, @NotNull @Positive Long amountCents) {}

    public record Payment(String id, String account, long amountCents, String chargeId) {}

    public record Refund(String id, String paymentId, long amountCents) {}
}
