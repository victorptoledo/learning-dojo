package dojo.fitness;

import java.util.List;

/** What a ratchet run found. Do not change it. */
public record RatchetResult(List<String> newViolations, List<String> fixed) {
    public boolean passes() {
        return newViolations.isEmpty();
    }
}
