package dojo;

import org.springframework.stereotype.Component;

/** Decides whether a flag is on for a user. */
@Component
public class Rollout {
    private final FlagStore flags;

    public Rollout(FlagStore flags) {
        this.flags = flags;
    }

    public boolean enabled(String flag, String user) {
        throw new UnsupportedOperationException("todo " + flag + " for " + user);
    }
}
