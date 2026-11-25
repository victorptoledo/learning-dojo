package dojo.billing;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Checked at startup: a wrong value stops the application before it takes any traffic. */
@ConfigurationProperties("billing")
@Validated
public record BillingProperties(
        @NotBlank @Pattern(regexp = "[A-Z]{2,5}") String invoicePrefix,
        @Min(1) @Max(90) int paymentTermDays,
        @NotBlank String formatter) {
}
