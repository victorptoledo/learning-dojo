package dojo.raft;

/** The node cannot serve the request as leader; `leaderHint` is the leader it knows of, or null. */
public class NotLeaderException extends RuntimeException {
    private final String leaderHint;

    public NotLeaderException(String leaderHint) {
        super(leaderHint == null ? "no known leader" : "the leader is " + leaderHint);
        this.leaderHint = leaderHint;
    }

    public String leaderHint() {
        return leaderHint;
    }
}
