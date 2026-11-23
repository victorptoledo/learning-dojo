package dojo.broker;

import java.util.List;

/** What one poll returned, and the group generation it belongs to (needed to commit). */
public record Batch(int generation, List<Message> messages) {
}
