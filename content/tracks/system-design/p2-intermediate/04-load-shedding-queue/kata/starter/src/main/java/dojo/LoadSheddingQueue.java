package dojo;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

/** A bounded work queue that sheds low-priority work and drops jobs past their deadline. */
public final class LoadSheddingQueue {

    public LoadSheddingQueue(int capacity, int sheddableLimit, Duration maxWait, Clock clock) {
        throw new UnsupportedOperationException("todo queue of " + capacity);
    }

    /** True if the job was queued. */
    public boolean offer(String job, Priority priority) {
        throw new UnsupportedOperationException("todo offer " + job + " as " + priority);
    }

    /** The next live job: critical first, oldest first. */
    public Optional<String> poll() {
        throw new UnsupportedOperationException("todo poll");
    }

    /** Live jobs queued. */
    public int size() {
        throw new UnsupportedOperationException("todo size");
    }

    public QueueStats stats() {
        throw new UnsupportedOperationException("todo stats");
    }
}
