package dojo.broker;

import java.util.Map;
import java.util.Optional;

/** A database of keys and values that survives a crash of the consumer. */
public interface Store {

    Optional<String> get(String key);

    /** Writes every entry, or none of them: one atomic transaction. */
    void apply(Map<String, String> writes);
}
