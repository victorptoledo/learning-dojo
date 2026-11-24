package dojo.domain;

/** A rule an applicant must meet, and the reason given when they do not. */
public record Requirement(Specification<Applicant> rule, String reason) {}
