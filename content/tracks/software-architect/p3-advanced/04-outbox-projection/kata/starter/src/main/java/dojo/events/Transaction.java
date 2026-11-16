package dojo.events;

import java.util.List;
import java.util.Map;

/** The working copies of the tables inside one transaction. Do not change it. */
public record Transaction(Map<String, OrderRow> orders, List<OutboxMessage> outbox) {}
