package dojo;

/** One partition changing owner. */
public record Move(int partition, String from, String to) {}
