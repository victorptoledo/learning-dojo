package dojo.shop.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Converts prices to euros, which is what the accounts are kept in. */
public class PriceConverter {
    private final ExchangeRates rates;

    public PriceConverter(ExchangeRates rates) {
        this.rates = rates;
    }

    public long toEuroCents(long cents, String currency) {
        if (currency.equals("EUR")) {
            return cents;
        }
        return rates.toEuro(currency).multiply(BigDecimal.valueOf(cents))
                .setScale(0, RoundingMode.HALF_EVEN)
                .longValueExact();
    }
}
