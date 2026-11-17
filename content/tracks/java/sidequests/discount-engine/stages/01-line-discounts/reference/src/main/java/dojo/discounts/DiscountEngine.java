package dojo.discounts;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/** Prices carts with discount rules read from text. */
public final class DiscountEngine {

    /** One kind of rule per record; the engine switches over them with patterns. */
    sealed interface Rule {
    }

    record PercentOff(long percent, Target target) implements Rule {
    }

    record FixedOff(long cents, String sku) implements Rule {
    }

    record BuyGet(long buy, long free, String sku) implements Rule {
    }

    record Bulk(long minimum, long unitCents, String sku) implements Rule {
    }

    record Over(long threshold, long cents) implements Rule {
    }

    record TierPercent(String tier, long percent) implements Rule {
    }

    record Cap(long percent) implements Rule {
    }

    /** What a percent rule matches. */
    sealed interface Target {
    }

    record Category(String name) implements Target {
    }

    record Sku(String sku) implements Target {
    }

    /** A rule, its trimmed text, its place in the rules text and its last valid day. */
    record Written(Rule rule, String text, int order, Optional<LocalDate> until) {
        boolean validOn(LocalDate day) {
            return day == null || until.isEmpty() || !day.isAfter(until.get());
        }
    }

    private final List<Written> rules;

    private DiscountEngine(List<Written> rules) {
        this.rules = rules;
    }

    public static DiscountEngine parse(String text) {
        List<Written> rules = new ArrayList<>();
        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int number = i + 1;
            String[] words = trimmed.split("\\s+");
            Optional<LocalDate> until = Optional.empty();
            if (words.length >= 2 && words[words.length - 2].equals("until")) {
                until = Optional.of(date(number, words[words.length - 1]));
                words = Arrays.copyOf(words, words.length - 2);
            }
            rules.add(new Written(rule(number, words), trimmed, rules.size(), until));
        }
        return new DiscountEngine(List.copyOf(rules));
    }

    private static Rule rule(int number, String[] w) {
        String shape = shape(w);
        return switch (w[0]) {
            case "percent" -> switch (shape) {
                case "percent N category W" -> new PercentOff(percent(number, w[1]), new Category(w[3]));
                case "percent N sku W" -> new PercentOff(percent(number, w[1]), new Sku(w[3]));
                default -> throw error(number, "expected `percent <n> category|sku <name>`");
            };
            case "fixed" -> {
                expect(number, shape, "fixed N sku W");
                yield new FixedOff(positive(number, w[1]), w[3]);
            }
            // stage 4
            case "cap" -> {
                expect(number, shape, "cap N");
                yield new Cap(percent(number, w[1]));
            }
            default -> throw error(number, "unknown rule `" + w[0] + "`");
        };
    }

    /** The words with every argument replaced: numbers by N, other words by W. */
    private static String shape(String[] words) {
        var shape = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            boolean keyword = List.of("category", "sku", "get", "price", "off", "percent").contains(words[i])
                    && i % 2 == 0;
            shape.append(' ').append(keyword ? words[i] : (words[i].matches("-?[0-9.]+") ? "N" : "W"));
        }
        return shape.toString();
    }

    private static void expect(int number, String shape, String wanted) {
        if (!shape.equals(wanted)) {
            throw error(number, "expected `" + wanted.replace(" N", " <n>").replace(" W", " <name>") + "`");
        }
    }

    private static long positive(int number, String word) {
        long value;
        try {
            value = Long.parseLong(word);
        } catch (NumberFormatException e) {
            throw error(number, "`" + word + "` is not a whole number");
        }
        if (value <= 0) {
            throw error(number, "`" + word + "` must be positive");
        }
        return value;
    }

    private static long percent(int number, String word) {
        long value = positive(number, word);
        if (value > 100) {
            throw error(number, "a percentage is at most 100, not " + value);
        }
        return value;
    }

    private static LocalDate date(int number, String word) {
        try {
            return LocalDate.parse(word);
        } catch (DateTimeParseException e) {
            throw error(number, "`" + word + "` is not a date (yyyy-mm-dd)");
        }
    }

    private static IllegalArgumentException error(int number, String message) {
        return new IllegalArgumentException("line " + number + ": " + message);
    }

    public Receipt price(List<Line> lines, String tier) {
        return priceOn(lines, tier, null);
    }

    private Receipt priceOn(List<Line> lines, String tier, LocalDate day) {
        List<Written> active = rules.stream().filter(rule -> rule.validOn(day)).toList();
        var applied = new TreeSet<Integer>();
        long subtotal = lines.stream().mapToLong(line -> line.unitCents() * line.quantity()).sum();

        long lineDiscounts = 0;
        for (Line line : lines) {
            Optional<Best> best = best(active, rule -> lineDiscount(rule, line));
            if (best.isPresent()) {
                lineDiscounts += best.get().amount();
                applied.add(best.get().order());
            }
        }
        long left = subtotal - lineDiscounts;

        final long afterLines = left;
        Optional<Best> over = best(active, rule -> rule instanceof Over(long threshold, long cents)
                && afterLines >= threshold ? cents : 0);
        if (over.isPresent()) {
            left = Math.max(0, left - over.get().amount());
            applied.add(over.get().order());
        }
        final long afterOver = left;
        Optional<Best> tierOff = best(active, rule -> rule instanceof TierPercent(String name, long percent)
                && name.equalsIgnoreCase(tier) ? afterOver * percent / 100 : 0);
        if (tierOff.isPresent()) {
            left -= tierOff.get().amount();
            applied.add(tierOff.get().order());
        }

        long discount = subtotal - left;
        Optional<Written> cap = active.stream()
                .filter(rule -> rule.rule() instanceof Cap)
                .min((a, b) -> Long.compare(((Cap) a.rule()).percent(), ((Cap) b.rule()).percent()));
        if (cap.isPresent()) {
            long most = subtotal * ((Cap) cap.get().rule()).percent() / 100;
            if (discount > most) {
                discount = most;
                applied.add(cap.get().order());
            }
        }
        List<String> texts = applied.stream().map(order -> rules.get(order).text()).toList();
        return new Receipt(subtotal, discount, subtotal - discount, texts);
    }

    private record Best(long amount, int order) {
    }

    /** The rule giving the largest positive amount; on a tie, the first one. */
    private static Optional<Best> best(List<Written> rules, java.util.function.ToLongFunction<Rule> amount) {
        Best best = null;
        for (Written rule : rules) {
            long value = amount.applyAsLong(rule.rule());
            if (value > 0 && (best == null || value > best.amount())) {
                best = new Best(value, rule.order());
            }
        }
        return Optional.ofNullable(best);
    }

    private static long lineDiscount(Rule rule, Line line) {
        long total = line.unitCents() * line.quantity();
        return switch (rule) {
            case PercentOff(long percent, Category(String name)) when name.equals(line.category()) ->
                    total * percent / 100;
            case PercentOff(long percent, Sku(String sku)) when sku.equals(line.sku()) -> total * percent / 100;
            case FixedOff(long cents, String sku) when sku.equals(line.sku()) ->
                    Math.min(cents, line.unitCents()) * line.quantity();
            case BuyGet(long buy, long free, String sku) when sku.equals(line.sku()) ->
                    line.quantity() / (buy + free) * free * line.unitCents();
            case Bulk(long minimum, long unitCents, String sku)
                    when sku.equals(line.sku()) && line.quantity() >= minimum ->
                    Math.max(0, line.unitCents() - unitCents) * line.quantity();
            default -> 0;
        };
    }
}
