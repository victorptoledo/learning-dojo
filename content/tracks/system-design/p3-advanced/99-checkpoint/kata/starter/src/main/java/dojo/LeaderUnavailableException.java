package dojo;

/** The partition needs its leader (or a new one) and has none that can answer. */
public class LeaderUnavailableException extends RuntimeException {
    public LeaderUnavailableException(int partition) {
        super("partition " + partition + " has no leader that can answer");
    }
}
