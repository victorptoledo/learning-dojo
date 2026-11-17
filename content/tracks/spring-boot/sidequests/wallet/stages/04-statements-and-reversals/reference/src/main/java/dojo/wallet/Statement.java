package dojo.wallet;

import java.time.YearMonth;

/** The monthly statement of an account. */
public record Statement(YearMonth month, long openingBalanceCents, long closingBalanceCents, long inCents,
        long outCents, long entries) {
}
