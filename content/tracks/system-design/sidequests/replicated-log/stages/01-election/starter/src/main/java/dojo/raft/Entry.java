package dojo.raft;

/** One log entry: the term of the leader that created it, and the command. */
public record Entry(long term, String command) {
}
