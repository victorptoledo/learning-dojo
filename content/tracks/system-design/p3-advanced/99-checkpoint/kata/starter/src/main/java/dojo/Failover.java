package dojo;

/** What a failover did: who leads now, in which epoch, and how many acknowledged writes were lost. */
public record Failover(int partition, String oldLeader, String newLeader, long epoch, long lostWrites) {}
