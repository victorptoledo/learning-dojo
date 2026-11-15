package dojo.legacy;

import java.util.Map;

/**
 * The treasury department's rate feed, written in 2009. You cannot change it. Ask for a pair
 * such as "EURUSD" and it answers "EURUSD=1.0842", or "N/A" when it has no quote for that
 * pair. It only knows each pair in one direction.
 */
public class LegacyFxFeed {
    private static final Map<String, String> QUOTES = Map.of(
            "EURUSD", "1.0842",
            "GBPEUR", "1.1650",
            "USDJPY", "151.20");

    public String quote(String pair) {
        String rate = QUOTES.get(pair);
        return rate == null ? "N/A" : pair + "=" + rate;
    }
}
