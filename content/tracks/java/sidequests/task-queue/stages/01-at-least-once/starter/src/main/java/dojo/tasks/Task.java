package dojo.tasks;

/** One delivery of a task: `attempt` is 1 on the first delivery, 2 on the second... */
public record Task(String id, String type, String payload, int attempt) {
}
