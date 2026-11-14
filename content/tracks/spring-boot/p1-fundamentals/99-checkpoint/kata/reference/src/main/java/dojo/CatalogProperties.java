package dojo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings under "catalog" in application.properties. Make it a validated
 * @ConfigurationProperties record (see the README) and register it.
 */
@ConfigurationProperties("catalog")
@Validated
public record CatalogProperties(
        @NotBlank String currency,
        @Min(1) @Max(100) int maxPageSize,
        @Min(0) @Max(90) int maxDiscountPercent) {}
