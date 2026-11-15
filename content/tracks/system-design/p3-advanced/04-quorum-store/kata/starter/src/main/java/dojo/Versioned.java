package dojo;

/** A value with the version the coordinator gave it; higher versions are newer. */
public record Versioned(String value, long version) {}
