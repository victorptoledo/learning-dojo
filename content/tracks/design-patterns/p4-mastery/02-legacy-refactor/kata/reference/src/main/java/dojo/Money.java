package dojo;

public final class Money {
    private Money() {}

    public static String format(long cents) {
        return "%d.%02d".formatted(cents / 100, cents % 100);
    }
}
