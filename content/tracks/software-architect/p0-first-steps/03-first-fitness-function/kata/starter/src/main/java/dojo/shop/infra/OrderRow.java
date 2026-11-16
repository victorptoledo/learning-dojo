package dojo.shop.infra;

/** The shape the order table stores. Do not change it. */
public record OrderRow(String id, int lineCount, long totalEuroCents) {}
