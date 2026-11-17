package dojo.discounts;

/** One line of a cart: what, which category, the price of one unit in cents, how many. */
public record Line(String sku, String category, long unitCents, int quantity) {
}
