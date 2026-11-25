package dojo;

/**
 * Parses "id;customerId;sku;quantity;priceCents". Called for every line: the hot path, so it
 * allocates only the three strings and the order it returns.
 */
public final class OrderParser {
    private OrderParser() {}

    public static Order parse(String line) {
        if (line == null || line.isEmpty()) {
            throw new BadOrderException("empty line");
        }
        int s1 = line.indexOf(';');
        int s2 = s1 < 0 ? -1 : line.indexOf(';', s1 + 1);
        int s3 = s2 < 0 ? -1 : line.indexOf(';', s2 + 1);
        int s4 = s3 < 0 ? -1 : line.indexOf(';', s3 + 1);
        if (s4 < 0 || line.indexOf(';', s4 + 1) >= 0) {
            throw new BadOrderException("expected 5 fields separated by ';'");
        }
        if (!isToken(line, 0, s1)) {
            throw new BadOrderException("bad order id '" + line.substring(0, s1) + "'");
        }
        if (!isToken(line, s1 + 1, s2)) {
            throw new BadOrderException("bad customer id '" + line.substring(s1 + 1, s2) + "'");
        }
        if (!isSku(line, s2 + 1, s3)) {
            throw new BadOrderException("bad sku '" + line.substring(s2 + 1, s3) + "'");
        }
        int quantity;
        long priceCents;
        try {
            quantity = Integer.parseInt(line, s3 + 1, s4, 10);
            priceCents = Long.parseLong(line, s4 + 1, line.length(), 10);
        } catch (NumberFormatException e) {
            throw new BadOrderException("bad number in '" + line + "'", e);
        }
        if (quantity < 1 || quantity > 1_000) {
            throw new BadOrderException("quantity must be 1 to 1000, got " + quantity);
        }
        if (priceCents < 0 || priceCents > 10_000_000) {
            throw new BadOrderException("price must be 0 to 10000000 cents, got " + priceCents);
        }
        return new Order(line.substring(0, s1), line.substring(s1 + 1, s2), line.substring(s2 + 1, s3),
                quantity, priceCents);
    }

    private static boolean isToken(String line, int from, int to) {
        if (from >= to) {
            return false;
        }
        for (int i = from; i < to; i++) {
            if (Character.isWhitespace(line.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** Three upper-case letters, a dash and four digits: "ABC-1234". */
    private static boolean isSku(String line, int from, int to) {
        if (to - from != 8 || line.charAt(from + 3) != '-') {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            char c = line.charAt(from + i);
            if (c < 'A' || c > 'Z') {
                return false;
            }
        }
        for (int i = 4; i < 8; i++) {
            char c = line.charAt(from + i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
