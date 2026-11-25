package dojo;

import java.math.BigDecimal;

/** The exchange-rates service, a remote dependency. */
public interface RateProvider {
    /**
     * How many {@code to} one {@code from} buys.
     *
     * @throws TransientException when the service times out or is down (worth retrying)
     * @throws UnknownCurrencyException when it does not know the pair (not worth retrying)
     */
    BigDecimal rate(String from, String to);
}
