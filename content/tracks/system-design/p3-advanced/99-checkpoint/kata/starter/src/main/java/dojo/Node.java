package dojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A copy of one partition: its replication log and the data the log produces. The leader appends;
 * followers apply the leader's entries in order. Given: do not change it.
 */
public final class Node {
    private final String name;
    private final List<LogEntry> log = new ArrayList<>();
    private final Map<String, String> data = new HashMap<>();
    private boolean down;
    private int reads;

    public Node(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    /** Position of the last entry in this node's log (0 when empty). Works while down. */
    public long position() {
        return log.size();
    }

    /** Epoch of the last entry in this node's log (0 when empty). Works while down. */
    public long lastEpoch() {
        return log.isEmpty() ? 0 : log.get(log.size() - 1).epoch();
    }

    /** As a leader: writes the next entry of the log, in the given epoch. */
    public LogEntry append(long epoch, String key, String value) {
        checkUp();
        LogEntry entry = new LogEntry(epoch, log.size() + 1, key, value);
        log.add(entry);
        data.put(key, value);
        return entry;
    }

    /** As a follower: applies the leader's entry that comes right after this node's position. */
    public void apply(LogEntry entry) {
        checkUp();
        if (entry.position() != log.size() + 1) {
            throw new IllegalStateException(name + " is at " + log.size() + " and cannot apply entry " + entry.position());
        }
        log.add(entry);
        data.put(entry.key(), entry.value());
    }

    /** Up to {@code max} entries after a position, in order. */
    public List<LogEntry> entriesAfter(long position, int max) {
        checkUp();
        int from = (int) Math.min(position, log.size());
        int to = (int) Math.min(log.size(), from + (long) max);
        return List.copyOf(log.subList(from, to));
    }

    public Optional<String> get(String key) {
        checkUp();
        reads++;
        return Optional.ofNullable(data.get(key));
    }

    /** Drops every entry after {@code position} and rebuilds the data; returns how many went. */
    public int truncateTo(long position) {
        checkUp();
        int removed = 0;
        while (log.size() > position) {
            log.remove(log.size() - 1);
            removed++;
        }
        data.clear();
        for (LogEntry entry : log) {
            data.put(entry.key(), entry.value());
        }
        return removed;
    }

    /** How many reads this node has served. Works while down. */
    public int reads() {
        return reads;
    }

    public void crash() {
        down = true;
    }

    public void restart() {
        down = false;
    }

    public boolean isDown() {
        return down;
    }

    private void checkUp() {
        if (down) {
            throw new NodeDownException(name);
        }
    }
}
