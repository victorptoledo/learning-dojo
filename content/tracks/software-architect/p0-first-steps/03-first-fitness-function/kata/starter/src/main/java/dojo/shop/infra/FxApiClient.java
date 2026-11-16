package dojo.shop.infra;

import java.math.BigDecimal;
import java.util.Map;

/** Stands in for a currency HTTP API: in production it needs a network and a key. Do not change it. */
public class FxApiClient {
    private static final Map<String, BigDecimal> QUOTES = Map.of(
            "USDEUR", new BigDecimal("0.92"),
            "GBPEUR", new BigDecimal("1.17"),
            "BRLEUR", new BigDecimal("0.17"));

    /** The price of one unit of the first currency in the second, for a pair such as "USDEUR". */
    public BigDecimal quote(String pair) {
        BigDecimal rate = QUOTES.get(pair);
        if (rate == null) {
            throw new IllegalArgumentException("no quote for " + pair);
        }
        return rate;
    }
}
