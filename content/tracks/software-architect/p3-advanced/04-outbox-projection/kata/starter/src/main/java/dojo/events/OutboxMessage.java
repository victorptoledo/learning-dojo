package dojo.events;

/** A row of the outbox table. Do not change it. */
public record OutboxMessage(Event event, boolean sent) {}
