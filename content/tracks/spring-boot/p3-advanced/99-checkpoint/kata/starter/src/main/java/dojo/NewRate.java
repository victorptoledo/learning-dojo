package dojo;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record NewRate(@NotNull @Positive @Digits(integer = 12, fraction = 6) BigDecimal perEuro) {
}
