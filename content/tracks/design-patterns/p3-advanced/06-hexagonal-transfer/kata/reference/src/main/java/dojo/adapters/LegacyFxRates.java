package dojo.adapters;

import dojo.domain.ExchangeRates;
import dojo.domain.RateUnavailableException;
import dojo.legacy.LegacyFxFeed;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Anti-corruption layer: turns the 2009 feed's "EURUSD=1.0842" strings and "N/A" answers into
 * the domain's BigDecimal rates and exceptions, and inverts pairs the feed knows only one way.
 */
public final class LegacyFxRates implements ExchangeRates {
    private final LegacyFxFeed feed;

    public LegacyFxRates(LegacyFxFeed feed) {
        this.feed = feed;
    }

    @Override
    public BigDecimal rate(String from, String to) {
        BigDecimal direct = parse(feed.quote(from + to));
        if (direct != null) {
            return direct;
        }
        BigDecimal inverse = parse(feed.quote(to + from));
        if (inverse != null) {
            return BigDecimal.ONE.divide(inverse, MathContext.DECIMAL64).setScale(6, RoundingMode.HALF_EVEN);
        }
        throw new RateUnavailableException(from + " to " + to);
    }

    private static BigDecimal parse(String answer) {
        int equals = answer.indexOf('=');
        return equals < 0 ? null : new BigDecimal(answer.substring(equals + 1));
    }
}
