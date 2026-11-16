package dojo.shop.domain;

import java.math.BigDecimal;

/** A port: what the domain needs to know about currencies, in its own words. */
public interface ExchangeRates {
    /** How many euros one unit of {@code currency} is worth. */
    BigDecimal toEuro(String currency);
}
