package dojo.domain;

/** One line of a transcript, in grade points: A is 4.0, F is 0.0. */
public record Grade(String course, double points) {}
