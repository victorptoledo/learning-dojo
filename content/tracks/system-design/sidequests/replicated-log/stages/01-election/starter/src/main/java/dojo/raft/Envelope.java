package dojo.raft;

/** A message on its way from one node to another. */
public record Envelope(String from, String to, Msg msg) {
}
