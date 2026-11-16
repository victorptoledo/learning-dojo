package dojo.fitness;

import java.util.Set;
import java.util.TreeSet;

/** Lets known violations stay, fails on new ones. */
public final class Ratchet {
    private Ratchet() {}

    public static RatchetResult check(Set<String> baseline, Set<String> current) {
        Set<String> added = new TreeSet<>(current);
        added.removeAll(baseline);
        Set<String> fixed = new TreeSet<>(baseline);
        fixed.removeAll(current);
        return new RatchetResult(java.util.List.copyOf(added), java.util.List.copyOf(fixed));
    }
}
