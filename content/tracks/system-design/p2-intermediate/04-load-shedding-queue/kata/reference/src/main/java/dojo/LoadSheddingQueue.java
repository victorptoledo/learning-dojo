package dojo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/** A bounded work queue that sheds low-priority work and drops jobs past their deadline. */
public final class LoadSheddingQueue {
    private record Job(String id, Instant offeredAt) {}

    private final int capacity;
    private final int sheddableLimit;
    private final Duration maxWait;
    private final Clock clock;
    private final Deque<Job> critical = new ArrayDeque<>();
    private final Deque<Job> sheddable = new ArrayDeque<>();
    private long accepted;
    private long rejected;
    private long shed;
    private long expired;
    private long served;

    public LoadSheddingQueue(int capacity, int sheddableLimit, Duration maxWait, Clock clock) {
        if (capacity < 1 || sheddableLimit < 0 || sheddableLimit > capacity) {
            throw new IllegalArgumentException("need capacity >= 1 and 0 <= sheddableLimit <= capacity");
        }
        if (maxWait.isZero() || maxWait.isNegative()) {
            throw new IllegalArgumentException("maxWait must be positive");
        }
        this.capacity = capacity;
        this.sheddableLimit = sheddableLimit;
        this.maxWait = maxWait;
        this.clock = clock;
    }

    /** True if the job was queued. */
    public synchronized boolean offer(String job, Priority priority) {
        Instant now = clock.instant();
        dropExpired(now);
        int queued = critical.size() + sheddable.size();
        boolean ok;
        if (priority == Priority.SHEDDABLE) {
            ok = queued < sheddableLimit;
            if (ok) {
                sheddable.addLast(new Job(job, now));
            }
        } else {
            if (queued >= capacity && !sheddable.isEmpty()) {
                sheddable.pollLast();
                shed++;
                queued--;
            }
            ok = queued < capacity;
            if (ok) {
                critical.addLast(new Job(job, now));
            }
        }
        if (ok) {
            accepted++;
        } else {
            rejected++;
        }
        return ok;
    }

    /** The next live job: critical first, oldest first. */
    public synchronized Optional<String> poll() {
        dropExpired(clock.instant());
        Job next = !critical.isEmpty() ? critical.pollFirst() : sheddable.pollFirst();
        if (next == null) {
            return Optional.empty();
        }
        served++;
        return Optional.of(next.id());
    }

    /** Live jobs queued. */
    public synchronized int size() {
        dropExpired(clock.instant());
        return critical.size() + sheddable.size();
    }

    public synchronized QueueStats stats() {
        return new QueueStats(accepted, rejected, shed, expired, served);
    }

    private void dropExpired(Instant now) {
        for (Deque<Job> queue : java.util.List.of(critical, sheddable)) {
            while (!queue.isEmpty() && !now.isBefore(queue.peekFirst().offeredAt().plus(maxWait))) {
                queue.pollFirst();
                expired++;
            }
        }
    }
}
