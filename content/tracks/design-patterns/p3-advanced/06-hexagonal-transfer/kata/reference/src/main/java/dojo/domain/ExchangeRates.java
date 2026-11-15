package dojo.domain;

import java.math.BigDecimal;

/** Driven port: how many units of {@code to} one unit of {@code from} buys. */
@FunctionalInterface
public interface ExchangeRates {
    /** Throws {@link RateUnavailableException} when no rate is known. */
    BigDecimal rate(String from, String to);
}
