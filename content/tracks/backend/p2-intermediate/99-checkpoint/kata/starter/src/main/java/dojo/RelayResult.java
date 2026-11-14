package dojo;

/** One relay run: how many events went out, and whether the run stopped on a failure. */
public record RelayResult(int published, boolean failed) {}
