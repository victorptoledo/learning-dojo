package dojo.library.adapters;

import dojo.library.domain.Notifier;

/** Driven adapter: messages to members, on the console. */
public final class ConsoleNotifier implements Notifier {
    @Override
    public void send(String memberId, String message) {
        System.out.println("to " + memberId + ": " + message);
    }
}
