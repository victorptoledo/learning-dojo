package dojo;

/** Greeting shown when someone enters the dojo. */
public final class Greeter {
    private Greeter() {}

    public static String greet(String name) {
        String who = name == null || name.isBlank() ? "stranger" : name.strip();
        return "Welcome to the dojo, %s!".formatted(who);
    }
}
