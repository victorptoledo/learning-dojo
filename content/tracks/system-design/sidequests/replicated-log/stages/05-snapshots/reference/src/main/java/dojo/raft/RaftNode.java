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
        Storage.Persistent saved = storage.load();
        term = saved.term();
        votedFor = saved.votedFor();
        entries.addAll(saved.log());
        snapshot = saved.snapshot();
        data.putAll(snapshot.data());
        clients.putAll(snapshot.clients());
        commitIndex = snapshot.index();
        lastApplied = snapshot.index();
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
        if (role != Role.LEADER) {
            throw new NotLeaderException(leader);
        }
        entries.add(new Entry(term, command));
        persist();
        if (clusterSize == 1) {
            advanceCommit();
        }
        broadcastAppend();
        return lastIndex();
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
        return propose(client + " " + sequence + " " + operation);
    }

    public long lastApplied() {
        return lastApplied;
    }

    public Map<String, String> data() {
        return Map.copyOf(data);
    }

    public Optional<String> read(String key) {
        if (role != Role.LEADER) {
            throw new NotLeaderException(leader);
        }
        int recent = 1;
        for (String peer : peers) {
            if (ticks - lastAck.getOrDefault(peer, Long.MIN_VALUE / 2) <= config.electionTimeout()) {
                recent++;
            }
        }
        if (recent * 2 <= clusterSize) {
            throw new NotLeaderException(null);
        }
        if (commitIndex <= snapshot.index() ? snapshot.term() != term : termAt(commitIndex) != term) {
            throw new IllegalStateException("no entry of term " + term + " is committed yet");
        }
        return Optional.ofNullable(data.get(key));
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
        long next = nextIndex.getOrDefault(peer, lastIndex() + 1);
        if (next <= snapshot.index()) {
            send(peer, new Msg.InstallSnapshot(term, id, snapshot));
            return;
        }
        long prev = next - 1;
        List<Entry> batch = new ArrayList<>(entries.subList((int) (prev - snapshot.index()), entries.size()));
        send(peer, new Msg.AppendEntries(term, id, prev, termAt(prev), batch, commitIndex));
    }

    private void onAppendEntries(Msg.AppendEntries m) {
        if (m.term() < term) {
            send(m.leader(), new Msg.AppendReply(term, id, false, 0));
            return;
        }
        role = Role.FOLLOWER;
        leader = m.leader();
        electionElapsed = 0;
        long prev = m.prevLogIndex();
        if (prev > lastIndex() || (prev > snapshot.index() && termAt(prev) != m.prevLogTerm())) {
            send(m.leader(), new Msg.AppendReply(term, id, false, Math.min(lastIndex(), prev - 1)));
            return;
        }
        boolean changed = false;
        long index = prev;
        for (Entry entry : m.entries()) {
            index++;
            if (index <= snapshot.index()) {
                continue;
            }
            if (index <= lastIndex()) {
                if (termAt(index) == entry.term()) {
                    continue;
                }
                entries.subList((int) (index - snapshot.index() - 1), entries.size()).clear();
            }
            entries.add(entry);
            changed = true;
        }
        if (changed) {
            persist();
        }
        long match = prev + m.entries().size();
        if (m.leaderCommit() > commitIndex) {
            commitIndex = Math.max(commitIndex, Math.min(m.leaderCommit(), match));
            apply();
        }
        send(m.leader(), new Msg.AppendReply(term, id, true, match));
    }

    private void onAppendReply(Msg.AppendReply m) {
        if (role != Role.LEADER || m.term() != term) {
            return;
        }
        lastAck.put(m.follower(), ticks);
        if (m.success()) {
            long match = Math.max(matchIndex.getOrDefault(m.follower(), 0L), m.matchIndex());
            matchIndex.put(m.follower(), match);
            nextIndex.put(m.follower(), match + 1);
            advanceCommit();
            if (match < lastIndex()) {
                sendAppend(m.follower());
            }
        } else {
            long next = nextIndex.getOrDefault(m.follower(), lastIndex() + 1);
            nextIndex.put(m.follower(), Math.max(1, Math.min(next - 1, m.matchIndex() + 1)));
            sendAppend(m.follower());
        }
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
            execute(entryAt(lastApplied).command());
        }
        if (config.snapshotEvery() > 0 && lastApplied - snapshot.index() >= config.snapshotEvery()) {
            takeSnapshot();
        }
    }

    /** `<client> <sequence> set <key> <value>` or `... del <key>`; anything else changes nothing. */
    private void execute(String command) {
        String[] parts = command.split(" ", 5);
        if (parts.length < 4) {
            return;
        }
        long sequence;
        try {
            sequence = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }
        if (sequence <= clients.getOrDefault(parts[0], 0L)) {
            return;
        }
        if (parts[2].equals("set") && parts.length == 5) {
            data.put(parts[3], parts[4]);
        } else if (parts[2].equals("del") && parts.length == 4) {
            data.remove(parts[3]);
        } else {
            return;
        }
        clients.put(parts[0], sequence);
    }

    private void takeSnapshot() {
        long index = lastApplied;
        long snapTerm = termAt(index);
        entries.subList(0, (int) (index - snapshot.index())).clear();
        snapshot = new Snapshot(index, snapTerm, Map.copyOf(data), Map.copyOf(clients));
        persist();
    }

    private void onInstallSnapshot(Msg.InstallSnapshot m) {
        if (m.term() < term) {
            send(m.leader(), new Msg.AppendReply(term, id, false, 0));
            return;
        }
        role = Role.FOLLOWER;
        leader = m.leader();
        electionElapsed = 0;
        Snapshot incoming = m.snapshot();
        if (incoming.index() > snapshot.index()) {
            if (incoming.index() <= lastIndex() && termAt(incoming.index()) == incoming.term()) {
                entries.subList(0, (int) (incoming.index() - snapshot.index())).clear();
            } else {
                entries.clear();
            }
            snapshot = incoming;
            if (lastApplied < incoming.index()) {
                data.clear();
                data.putAll(incoming.data());
                clients.clear();
                clients.putAll(incoming.clients());
                lastApplied = incoming.index();
            }
            commitIndex = Math.max(commitIndex, incoming.index());
            persist();
        }
        send(m.leader(), new Msg.AppendReply(term, id, true, snapshot.index()));
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
        storage.save(new Storage.Persistent(term, votedFor, List.copyOf(entries), snapshot));
    }

    private void send(String to, Msg msg) {
        outbox.add(new Envelope(id, to, msg));
    }
}
