package dojo;

import java.time.Instant;

/** Something that happened in the shop. Events can arrive late, so ids and times disagree. */
public record Event(long id, String type, String subject, Instant occurredAt) {}
