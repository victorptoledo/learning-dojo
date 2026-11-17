package dojo.tasks;

import java.time.Clock;
import java.util.Optional;

/** A task queue with at-least-once delivery. See README.md. */
public final class TaskQueue {

    public TaskQueue(Clock clock, QueueConfig config) {
    }

    public String enqueue(String type, String payload) {
        throw new UnsupportedOperationException("todo: enqueue");
    }

    public Optional<Task> poll() {
        throw new UnsupportedOperationException("todo: poll");
    }

    public boolean ack(Task delivery) {
        throw new UnsupportedOperationException("todo: ack");
    }

    public boolean nack(Task delivery, String error) {
        throw new UnsupportedOperationException("todo: nack");
    }

    public Status status(String id) {
        throw new UnsupportedOperationException("todo: status");
    }
}
