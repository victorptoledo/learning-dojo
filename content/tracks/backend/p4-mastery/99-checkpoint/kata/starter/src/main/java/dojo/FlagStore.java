package dojo;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** The feature flags, as a flag service would hold them. Operators change them at run time. */
@Component
public class FlagStore {
    /** On for {@code percent}% of the users, and always on for the users in {@code allow}. */
    public record Flag(int percent, Set<String> allow) {}

    public static final String SAGA_CHECKOUT = "saga-checkout";

    private final Map<String, Flag> flags = new ConcurrentHashMap<>(Map.of(SAGA_CHECKOUT, new Flag(0, Set.of())));

    public Optional<Flag> get(String name) {
        return Optional.ofNullable(flags.get(name));
    }

    public void set(String name, int percent, Set<String> allow) {
        flags.put(name, new Flag(percent, Set.copyOf(allow)));
    }
}
