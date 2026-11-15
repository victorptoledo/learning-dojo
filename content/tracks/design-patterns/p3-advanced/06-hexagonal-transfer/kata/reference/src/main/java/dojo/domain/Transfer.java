package dojo.domain;

import java.math.BigDecimal;

/** What a completed transfer did. */
public record Transfer(String from, String to, long debitedCents, long creditedCents, BigDecimal rate) {}
