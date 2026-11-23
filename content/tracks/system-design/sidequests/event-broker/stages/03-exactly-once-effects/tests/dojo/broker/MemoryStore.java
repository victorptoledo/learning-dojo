package dojo.broker;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** A store in memory: it outlives the processors, like a database outlives a crashed consumer. */
final class MemoryStore implements Store {
    final Map<String, String> data = new HashMap<>();
    int transactions;

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(data.get(key));
    }

    @Override
    public void apply(Map<String, String> writes) {
        transactions++;
        data.putAll(new HashMap<>(writes));
    }

    long balance(String account) {
        return Long.parseLong(data.getOrDefault("balance:" + account, "0"));
    }

    /** The process dies: not an exception a consumer is supposed to catch. */
    static final class Crash extends Error {
        Crash(String message) {
            super(message);
        }
    }

    /** A handler that credits `account=amount` messages to `balance:<account>`. */
    static Map<String, String> credit(Message message, Store store) {
        String[] parts = message.value().split("=");
        long now = Long.parseLong(store.get("balance:" + parts[0]).orElse("0"));
        return Map.of("balance:" + parts[0], Long.toString(now + Long.parseLong(parts[1])));
    }
}
