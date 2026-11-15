package dojo;

import java.util.List;
import java.util.Optional;

/** The coordinator of a leaderless store with N replicas, write quorum W and read quorum R. */
public final class QuorumStore {

    public QuorumStore(List<Replica> replicas, int w, int r) {
        throw new UnsupportedOperationException("todo store with W=" + w + " R=" + r);
    }

    /** True when every read quorum overlaps every write quorum (W + R > N). */
    public boolean overlapping() {
        throw new UnsupportedOperationException("todo overlapping");
    }

    /** Writes to every reachable replica; fails with fewer than W acknowledgements. */
    public long put(String key, String value) {
        throw new UnsupportedOperationException("todo put " + key);
    }

    /** Reads R replicas, returns the newest value and repairs the stale ones. */
    public Optional<String> get(String key) {
        throw new UnsupportedOperationException("todo get " + key);
    }

    /** Repair writes sent by reads so far. */
    public long readRepairs() {
        throw new UnsupportedOperationException("todo read repairs");
    }
}
