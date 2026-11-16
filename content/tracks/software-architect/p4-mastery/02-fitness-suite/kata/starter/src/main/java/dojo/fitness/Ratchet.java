package dojo.fitness;

import java.util.Set;

/** Lets known violations stay, fails on new ones. */
public final class Ratchet {
    private Ratchet() {}

    public static RatchetResult check(Set<String> baseline, Set<String> current) {
        throw new UnsupportedOperationException("todo: compare with the baseline");
    }
}
