package dojo;

/** What a session has seen of one partition: the epoch it saw it in and the log position. */
public record Token(long epoch, long position) {}
