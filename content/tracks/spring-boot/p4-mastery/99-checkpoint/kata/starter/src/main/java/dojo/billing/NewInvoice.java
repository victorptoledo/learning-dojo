package dojo.billing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record NewInvoice(@NotBlank String customer, @Positive long amountCents) {
}
