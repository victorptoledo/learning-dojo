package dojo.library.adapters;

/** Sends messages to members. Here, the console. */
public final class ConsoleNotifier {
    private ConsoleNotifier() {}

    public static void send(String memberId, String message) {
        System.out.println("to " + memberId + ": " + message);
    }
}
