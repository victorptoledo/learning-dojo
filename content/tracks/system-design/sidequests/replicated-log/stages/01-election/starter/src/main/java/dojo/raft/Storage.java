package dojo.raft;

import java.util.List;

/**
 * The disk of a node: what must survive a crash (used from stage 3). `save` returns once the
 * state is durable.
 */
public interface Storage {

    /** What a node saves: its term, its vote in that term, its log after the snapshot, and the snapshot. */
    record Persistent(long term, String votedFor, List<Entry> log, Snapshot snapshot) {

        public static final Persistent EMPTY = new Persistent(0, null, List.of(), Snapshot.EMPTY);
    }

    /** The last state saved, or `Persistent.EMPTY` on a new disk. */
    Persistent load();

    void save(Persistent state);
}
