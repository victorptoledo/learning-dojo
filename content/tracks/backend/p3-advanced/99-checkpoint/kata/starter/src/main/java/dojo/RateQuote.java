package dojo;

import java.math.BigDecimal;

/** A rate, and whether it is the last known value because the rates service could not answer. */
public record RateQuote(BigDecimal rate, boolean stale) {}
