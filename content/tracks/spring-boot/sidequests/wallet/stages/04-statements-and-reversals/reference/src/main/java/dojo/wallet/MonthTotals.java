package dojo.wallet;

/** What came in, what went out and how many entries, for one account and one month. */
public record MonthTotals(long inCents, long outCents, long entries) {
}
