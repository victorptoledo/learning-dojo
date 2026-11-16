package dojo;

/** One change: a put when {@code value} is not null, a delete when it is. */
public record Mutation(String key, String value) {
    public static Mutation put(String key, String value) {
        if (value == null) throw new IllegalArgumentException("a put needs a value");
        return new Mutation(key, value);
    }

    public static Mutation delete(String key) {
        return new Mutation(key, null);
    }
}
