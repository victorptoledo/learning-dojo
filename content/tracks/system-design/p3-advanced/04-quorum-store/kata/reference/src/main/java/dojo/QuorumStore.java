package dojo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** The coordinator of a leaderless store with N replicas, write quorum W and read quorum R. */
public final class QuorumStore {
    private record Answer(Replica replica, Optional<Versioned> value) {}

    private final List<Replica> replicas;
    private final int w;
    private final int r;
    private long lastVersion;
    private long repairs;

    public QuorumStore(List<Replica> replicas, int w, int r) {
        int n = replicas.size();
        if (n < 1 || w < 1 || w > n || r < 1 || r > n) {
            throw new IllegalArgumentException("need N >= 1 and 1 <= W, R <= N");
        }
        this.replicas = List.copyOf(replicas);
        this.w = w;
        this.r = r;
    }

    /** True when every read quorum overlaps every write quorum (W + R > N). */
    public boolean overlapping() {
        return w + r > replicas.size();
    }

    /** Writes to every reachable replica; fails with fewer than W acknowledgements. */
    public synchronized long put(String key, String value) {
        Versioned versioned = new Versioned(value, ++lastVersion);
        int acks = 0;
        for (Replica replica : replicas) {
            try {
                replica.write(key, versioned);
                acks++;
            } catch (ReplicaDownException e) {
                // an unreachable replica simply does not acknowledge
            }
        }
        if (acks < w) {
            throw new QuorumNotReachedException("write of " + key + ": " + acks + " of " + w + " acknowledgements");
        }
        return versioned.version();
    }

    /** Reads R replicas, returns the newest value and repairs the stale ones. */
    public synchronized Optional<String> get(String key) {
        List<Answer> answers = new ArrayList<>();
        for (Replica replica : replicas) {
            if (answers.size() == r) {
                break;
            }
            try {
                answers.add(new Answer(replica, replica.read(key)));
            } catch (ReplicaDownException e) {
                // try the next replica
            }
        }
        if (answers.size() < r) {
            throw new QuorumNotReachedException("read of " + key + ": " + answers.size() + " of " + r + " answers");
        }
        Optional<Versioned> newest = answers.stream()
                .flatMap(answer -> answer.value().stream())
                .max(Comparator.comparingLong(Versioned::version));
        newest.ifPresent(latest -> repair(key, latest, answers));
        return newest.map(Versioned::value);
    }

    /** Repair writes sent by reads so far. */
    public synchronized long readRepairs() {
        return repairs;
    }

    private void repair(String key, Versioned latest, List<Answer> answers) {
        for (Answer answer : answers) {
            boolean stale = answer.value().map(v -> v.version() < latest.version()).orElse(true);
            if (stale) {
                try {
                    answer.replica().write(key, latest);
                    repairs++;
                } catch (ReplicaDownException e) {
                    // it will be repaired by a later read
                }
            }
        }
    }
}
