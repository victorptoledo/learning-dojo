package dojo.raft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** One node of a Raft cluster, as a state machine. See README.md. */
public final class RaftNode {
    private final String id;
    private final List<String> peers;
    private final int clusterSize;
    private final RaftConfig config;
    private final Storage storage;
    private final List<Envelope> outbox = new ArrayList<>();

    // persistent state
    private long term;
    private String votedFor;
    private final List<Entry> entries = new ArrayList<>(); // the log after the snapshot
    private Snapshot snapshot = Snapshot.EMPTY;

    // volatile state
    private Role role = Role.FOLLOWER;
    private String leader;
    private long commitIndex;
    private long lastApplied;
    private int electionElapsed;
    private int heartbeatElapsed;
    private long ticks;
    private final Set<String> votes = new HashSet<>();
    private final Map<String, Long> nextIndex = new HashMap<>();
    private final Map<String, Long> matchIndex = new HashMap<>();
    private final Map<String, Long> lastAck = new HashMap<>();
    private final Map<String, String> data = new TreeMap<>();
    private final Map<String, Long> clients = new HashMap<>();

    public RaftNode(String id, List<String> members, RaftConfig config, Storage storage) {
        this.id = id;
        this.peers = members.stream().filter(m -> !m.equals(id)).sorted().toList();
        this.clusterSize = members.size();
        this.config = config;
        this.storage = storage;
    }

    // ---- driven by the cluster ----

    public void tick() {
        ticks++;
        if (role == Role.LEADER) {
            if (++heartbeatElapsed >= config.heartbeatInterval()) {
                heartbeatElapsed = 0;
                broadcastAppend();
            }
        } else if (++electionElapsed >= config.electionTimeout()) {
            startElection();
        }
    }

    public void receive(Envelope envelope) {
        Msg msg = envelope.msg();
        if (msg.term() > term) {
            term = msg.term();
            votedFor = null;
            role = Role.FOLLOWER;
            leader = null;
            persist();
        }
        switch (msg) {
            case Msg.RequestVote m -> onRequestVote(m);
            case Msg.Vote m -> onVote(m);
            case Msg.AppendEntries m -> onAppendEntries(m);
            case Msg.AppendReply m -> onAppendReply(m);
            case Msg.InstallSnapshot m -> onInstallSnapshot(m);
        }
    }

    public List<Envelope> drainOutbox() {
        List<Envelope> out = List.copyOf(outbox);
        outbox.clear();
        return out;
    }

    // ---- what the node knows ----

    public Role role() {
        return role;
    }

    public long term() {
        return term;
    }

    public String votedFor() {
        return votedFor;
    }

    public String leader() {
        return leader;
    }

    // ---- the log ----

    public long propose(String command) {
        throw new UnsupportedOperationException("todo: propose (stage 2)");
    }

    public List<Entry> log() {
        return List.copyOf(entries);
    }

    public long lastIndex() {
        return snapshot.index() + entries.size();
    }

    public long commitIndex() {
        return commitIndex;
    }

    // ---- the key-value store ----

    public long submit(String client, long sequence, String operation) {
        throw new UnsupportedOperationException("todo: submit (stage 4)");
    }

    public long lastApplied() {
        return lastApplied;
    }

    public Map<String, String> data() {
        return Map.copyOf(data);
    }

    public Optional<String> read(String key) {
        throw new UnsupportedOperationException("todo: read (stage 4)");
    }

    public long snapshotIndex() {
        return snapshot.index();
    }

    // ---- elections ----

    private void startElection() {
        term++;
        role = Role.CANDIDATE;
        votedFor = id;
        leader = null;
        votes.clear();
        votes.add(id);
        electionElapsed = 0;
        persist();
        if (votes.size() * 2 > clusterSize) {
            becomeLeader();
            return;
        }
        for (String peer : peers) {
            send(peer, new Msg.RequestVote(term, id, lastIndex(), termAt(lastIndex())));
        }
    }

    private void onRequestVote(Msg.RequestVote m) {
        boolean upToDate = m.lastLogTerm() > termAt(lastIndex())
                || (m.lastLogTerm() == termAt(lastIndex()) && m.lastLogIndex() >= lastIndex());
        boolean grant = m.term() == term && (votedFor == null || votedFor.equals(m.candidate())) && upToDate;
        if (grant) {
            votedFor = m.candidate();
            electionElapsed = 0;
            persist();
        }
        send(m.candidate(), new Msg.Vote(term, id, grant));
    }

    private void onVote(Msg.Vote m) {
        if (role != Role.CANDIDATE || m.term() != term || !m.granted()) {
            return;
        }
        votes.add(m.voter());
        if (votes.size() * 2 > clusterSize) {
            becomeLeader();
        }
    }

    private void becomeLeader() {
        role = Role.LEADER;
        leader = id;
        heartbeatElapsed = 0;
        for (String peer : peers) {
            nextIndex.put(peer, lastIndex() + 1);
            matchIndex.put(peer, 0L);
            lastAck.remove(peer);
        }
        broadcastAppend();
    }

    // ---- replication ----

    private void broadcastAppend() {
        for (String peer : peers) {
            sendAppend(peer);
        }
    }

    private void sendAppend(String peer) {
        send(peer, new Msg.AppendEntries(term, id, 0, 0, List.of(), 0));
    }

    private void onAppendEntries(Msg.AppendEntries m) {
        if (m.term() < term) {
            send(m.leader(), new Msg.AppendReply(term, id, false, 0));
            return;
        }
        role = Role.FOLLOWER;
        leader = m.leader();
        electionElapsed = 0;
        send(m.leader(), new Msg.AppendReply(term, id, true, 0));
    }

    private void onAppendReply(Msg.AppendReply m) {
        if (role != Role.LEADER || m.term() != term) {
            return;
        }
        lastAck.put(m.follower(), ticks);
    }

    /** Commits the highest entry of this term that a majority has. */
    private void advanceCommit() {
        for (long n = lastIndex(); n > commitIndex && n > snapshot.index(); n--) {
            if (termAt(n) != term) {
                break;
            }
            int have = 1;
            for (String peer : peers) {
                if (matchIndex.getOrDefault(peer, 0L) >= n) {
                    have++;
                }
            }
            if (have * 2 > clusterSize) {
                commitIndex = n;
                apply();
                return;
            }
        }
    }

    // ---- the state machine ----

    private void apply() {
        while (lastApplied < commitIndex) {
            lastApplied++;
        }
    }

    private void onInstallSnapshot(Msg.InstallSnapshot m) {
        throw new UnsupportedOperationException("snapshots come in stage 5");
    }

    // ---- helpers ----

    /** The term of the entry at the index; 0 for index 0. */
    private long termAt(long index) {
        if (index == snapshot.index()) {
            return snapshot.term();
        }
        if (index < snapshot.index() || index > lastIndex()) {
            return 0;
        }
        return entryAt(index).term();
    }

    private Entry entryAt(long index) {
        return entries.get((int) (index - snapshot.index() - 1));
    }

    private void persist() {
    }

    private void send(String to, Msg msg) {
        outbox.add(new Envelope(id, to, msg));
    }
}
