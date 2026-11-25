package dojo;

import java.util.regex.Pattern;

/** Parses "id;customerId;sku;quantity;priceCents". Called for every line: the hot path. */
public final class OrderParser {
    private OrderParser() {}

    public static Order parse(String line) {
        if (line == null || line.isEmpty()) {
            throw new BadOrderException("empty line");
        }
        String[] parts = line.split(";", -1);
        if (parts.length != 5) {
            throw new BadOrderException(String.format("expected 5 fields separated by ';', got %d", parts.length));
        }
        String id = parts[0];
        String customerId = parts[1];
        if (id.isEmpty() || id.chars().anyMatch(Character::isWhitespace)) {
            throw new BadOrderException(String.format("bad order id '%s'", id));
        }
        if (customerId.isEmpty() || customerId.chars().anyMatch(Character::isWhitespace)) {
            throw new BadOrderException(String.format("bad customer id '%s'", customerId));
        }
        String sku = parts[2];
        if (!Pattern.compile("[A-Z]{3}-[0-9]{4}").matcher(sku).matches()) {
            throw new BadOrderException(String.format("bad sku '%s'", sku));
        }
        int quantity;
        long priceCents;
        try {
            quantity = Integer.parseInt(parts[3]);
            priceCents = Long.parseLong(parts[4]);
        } catch (NumberFormatException e) {
            throw new BadOrderException(String.format("bad number in '%s'", line), e);
        }
        if (quantity < 1 || quantity > 1_000) {
            throw new BadOrderException(String.format("quantity must be 1 to 1000, got %d", quantity));
        }
        if (priceCents < 0 || priceCents > 10_000_000) {
            throw new BadOrderException(String.format("price must be 0 to 10000000 cents, got %d", priceCents));
        }
        return new Order(id, customerId, sku, quantity, priceCents);
    }
}
