package dojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(@NotBlank String account, @NotNull @Positive Long amountCents) {
    /** Two requests are "the same" when this is equal. */
    public String fingerprint() {
        return account.strip() + ":" + amountCents;
    }
}
