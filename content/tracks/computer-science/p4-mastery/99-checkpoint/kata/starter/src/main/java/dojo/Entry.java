package dojo;

/** One key and its value, as {@link BPlusTree#range} returns them. */
public record Entry(long key, long value) {}
