package dojo.discounts;

import java.util.List;

/** A priced cart, in cents, and the rules that changed the price (as written). */
public record Receipt(long subtotalCents, long discountCents, long totalCents, List<String> applied) {
}
