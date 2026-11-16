package dojo.shop.domain;

import dojo.shop.infra.FxApiClient;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Converts prices to euros, which is what the accounts are kept in. */
public class PriceConverter {
    private final FxApiClient client = new FxApiClient();

    public long toEuroCents(long cents, String currency) {
        if (currency.equals("EUR")) {
            return cents;
        }
        BigDecimal rate = client.quote(currency + "EUR");
        return rate.multiply(BigDecimal.valueOf(cents))
                .setScale(0, RoundingMode.HALF_EVEN)
                .longValueExact();
    }
}
