package dojo;

/**
 * Settings under "catalog" in application.properties. Make it a validated
 * @ConfigurationProperties record (see the README) and register it.
 */
public record CatalogProperties(String currency, int maxPageSize, int maxDiscountPercent) {}
