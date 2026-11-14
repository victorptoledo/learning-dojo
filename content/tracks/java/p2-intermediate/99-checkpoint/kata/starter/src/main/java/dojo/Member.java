package dojo;

/** A library member who may hold at most {@code maxLoans} books at once. */
public record Member(String id, String name, int maxLoans) implements Identified<String> {}
