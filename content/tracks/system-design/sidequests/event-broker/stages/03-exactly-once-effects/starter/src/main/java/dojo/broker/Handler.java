package dojo.broker;

import java.util.Map;

/** What a message does: it reads the store if it needs to and returns the writes to make. */
@FunctionalInterface
public interface Handler {

    Map<String, String> handle(Message message, Store store);
}
