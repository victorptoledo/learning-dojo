package dojo.shop.infra;

import dojo.shop.domain.ExchangeRates;
import java.math.BigDecimal;

/** The adapter: the domain's port, implemented with the currency API. */
public class FxApiRates implements ExchangeRates {
    private final FxApiClient client;

    public FxApiRates(FxApiClient client) {
        this.client = client;
    }

    @Override
    public BigDecimal toEuro(String currency) {
        return client.quote(currency + "EUR");
    }
}
