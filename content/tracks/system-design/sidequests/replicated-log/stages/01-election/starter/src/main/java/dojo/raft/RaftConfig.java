package dojo.raft;

/**
 * The timing of one node, in ticks: a follower that hears nothing for `electionTimeout`
 * ticks starts an election, and a leader sends heartbeats every `heartbeatInterval` ticks.
 * From stage 5, a node takes a snapshot once `snapshotEvery` entries were applied since the
 * last one (0 = never).
 */
public record RaftConfig(int electionTimeout, int heartbeatInterval, int snapshotEvery) {
}
