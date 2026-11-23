package dojo.raft;

import java.util.List;

/** The messages nodes send each other. Every message carries the sender's term. */
public sealed interface Msg {

    long term();

    /** A candidate asks for a vote, describing the end of its log. */
    record RequestVote(long term, String candidate, long lastLogIndex, long lastLogTerm) implements Msg {}

    /** The answer to a RequestVote. */
    record Vote(long term, String voter, boolean granted) implements Msg {}

    /**
     * A leader sends the entries after `prevLogIndex` (none for a heartbeat), with the term of
     * the entry at `prevLogIndex` (0 when it is 0), and how far the log is committed.
     */
    record AppendEntries(long term, String leader, long prevLogIndex, long prevLogTerm, List<Entry> entries,
            long leaderCommit) implements Msg {}

    /**
     * The answer to an AppendEntries or an InstallSnapshot. On success `matchIndex` is the last
     * index known to match the leader; on failure it is a hint of where to try next.
     */
    record AppendReply(long term, String follower, boolean success, long matchIndex) implements Msg {}

    /** A leader sends its snapshot to a follower that is behind it (stage 5). */
    record InstallSnapshot(long term, String leader, Snapshot snapshot) implements Msg {}
}
