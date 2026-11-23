package dojo.raft;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One node of a Raft cluster, as a state machine: it changes only when it is ticked or gets a
 * message, and it never talks to the network itself (the messages it sends wait in its
 * outbox). Each stage's README says which methods it needs.
 */
public final class RaftNode {

    public RaftNode(String id, List<String> members, RaftConfig config, Storage storage) {
    }

    // ---- driven by the cluster (stage 1) ----

    public void tick() {
        throw new UnsupportedOperationException("todo: tick");
    }

    public void receive(Envelope envelope) {
        throw new UnsupportedOperationException("todo: receive");
    }

    public List<Envelope> drainOutbox() {
        throw new UnsupportedOperationException("todo: drainOutbox");
    }

    // ---- what the node knows (stage 1) ----

    public Role role() {
        throw new UnsupportedOperationException("todo: role");
    }

    public long term() {
        throw new UnsupportedOperationException("todo: term");
    }

    public String votedFor() {
        throw new UnsupportedOperationException("todo: votedFor");
    }

    public String leader() {
        throw new UnsupportedOperationException("todo: leader");
    }

    // ---- the log (stage 2) ----

    public long propose(String command) {
        throw new UnsupportedOperationException("todo: propose");
    }

    public List<Entry> log() {
        throw new UnsupportedOperationException("todo: log");
    }

    public long lastIndex() {
        throw new UnsupportedOperationException("todo: lastIndex");
    }

    public long commitIndex() {
        throw new UnsupportedOperationException("todo: commitIndex");
    }

    // ---- the key-value store (stage 4) ----

    public long submit(String client, long sequence, String operation) {
        throw new UnsupportedOperationException("todo: submit");
    }

    public long lastApplied() {
        throw new UnsupportedOperationException("todo: lastApplied");
    }

    public Map<String, String> data() {
        throw new UnsupportedOperationException("todo: data");
    }

    public Optional<String> read(String key) {
        throw new UnsupportedOperationException("todo: read");
    }

    // ---- snapshots (stage 5; 0 until then) ----

    public long snapshotIndex() {
        throw new UnsupportedOperationException("todo: snapshotIndex");
    }
}
