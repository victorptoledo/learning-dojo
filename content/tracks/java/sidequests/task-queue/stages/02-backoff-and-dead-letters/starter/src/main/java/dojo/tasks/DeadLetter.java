package dojo.tasks;

/** A task that failed too often: its last delivery and the error of that delivery. */
public record DeadLetter(Task task, String lastError) {
}
