package dojo.tasks;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * A task queue with at-least-once delivery. Every method holds the queue's lock: simple and
 * correct; leases are checked lazily, whenever the state is read.
 */
public final class TaskQueue {

    private static final String LEASE_EXPIRED = "lease expired";

    private static final class Entry {
        final String id;
        final String type;
        final String payload;
        final String key;
        final long order;
        Status status = Status.READY;
        int attempts;
        Instant readyAt;
        Instant leaseUntil;
        Task current;

        Entry(String id, String type, String payload, String key, long order, Instant readyAt) {
            this.id = id;
            this.type = type;
            this.payload = payload;
            this.key = key;
            this.order = order;
            this.readyAt = readyAt;
        }
    }

    private final Clock clock;
    private final QueueConfig config;
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final List<DeadLetter> dead = new ArrayList<>();
    private long nextId = 1;

    public TaskQueue(Clock clock, QueueConfig config) {
        this.clock = clock;
        this.config = config;
    }

    public synchronized String enqueue(String type, String payload) {
        return add(type, payload, null);
    }

    // stage 3
    public synchronized String enqueue(String type, String payload, String idempotencyKey) {
        return add(type, payload, Objects.requireNonNull(idempotencyKey));
    }
    // end stage 3

    private String add(String type, String payload, String key) {
        Instant now = clock.instant();
        if (key != null) {
            for (Entry entry : entries.values()) {
                refresh(entry, now);
                if (key.equals(entry.key) && entry.status != Status.DEAD) {
                    return entry.id;
                }
            }
        }
        String id = "task-" + nextId;
        entries.put(id, new Entry(id, type, payload, key, nextId, now));
        nextId++;
        return id;
    }

    public synchronized Optional<Task> poll() {
        Instant now = clock.instant();
        entries.values().forEach(entry -> refresh(entry, now));
        Optional<Entry> next = entries.values().stream()
                .filter(entry -> entry.status == Status.READY && !entry.readyAt.isAfter(now))
                .min(Comparator.comparing((Entry entry) -> entry.readyAt).thenComparingLong(entry -> entry.order));
        next.ifPresent(entry -> {
            entry.status = Status.LEASED;
            entry.attempts++;
            entry.leaseUntil = now.plus(config.lease());
            entry.current = new Task(entry.id, entry.type, entry.payload, entry.attempts);
        });
        return next.map(entry -> entry.current);
    }

    public synchronized boolean ack(Task delivery) {
        Entry entry = current(delivery);
        if (entry == null) {
            return false;
        }
        entry.status = Status.DONE;
        entry.current = null;
        return true;
    }

    public synchronized boolean nack(Task delivery, String error) {
        Entry entry = current(delivery);
        if (entry == null) {
            return false;
        }
        Instant now = clock.instant();
        fail(entry, error, now.plus(backoff(entry.attempts)));
        return true;
    }

    public synchronized Status status(String id) {
        Entry entry = entries.get(id);
        if (entry == null) {
            throw new NoSuchElementException("no task " + id);
        }
        refresh(entry, clock.instant());
        return entry.status;
    }

    // stage 2
    public synchronized List<DeadLetter> deadLetters() {
        Instant now = clock.instant();
        entries.values().forEach(entry -> refresh(entry, now));
        return List.copyOf(dead);
    }

    public synchronized boolean requeue(String id) {
        Entry entry = entries.get(id);
        if (entry == null) {
            return false;
        }
        Instant now = clock.instant();
        refresh(entry, now);
        if (entry.status != Status.DEAD) {
            return false;
        }
        dead.removeIf(letter -> letter.task().id().equals(id));
        entry.status = Status.READY;
        entry.attempts = 0;
        entry.readyAt = now;
        return true;
    }
    // end stage 2

    /** The entry when `delivery` is its current, unexpired lease; otherwise null. */
    private Entry current(Task delivery) {
        Entry entry = entries.get(delivery.id());
        if (entry == null) {
            return null;
        }
        refresh(entry, clock.instant());
        return entry.status == Status.LEASED && delivery.equals(entry.current) ? entry : null;
    }

    /** A lease that ran out is a failed delivery, ready again when the lease ended. */
    private void refresh(Entry entry, Instant now) {
        if (entry.status == Status.LEASED && !now.isBefore(entry.leaseUntil)) {
            fail(entry, LEASE_EXPIRED, entry.leaseUntil);
        }
    }

    private void fail(Entry entry, String error, Instant readyAgain) {
        if (entry.attempts >= config.maxAttempts()) {
            entry.status = Status.DEAD;
            dead.add(new DeadLetter(entry.current, error));
        } else {
            entry.status = Status.READY;
            entry.readyAt = readyAgain;
        }
        entry.current = null;
        entry.leaseUntil = null;
    }

    /** baseDelay × 2^(attempt-1), at most maxDelay. */
    private Duration backoff(int attempt) {
        Duration delay = config.baseDelay();
        for (int i = 1; i < attempt && delay.compareTo(config.maxDelay()) < 0; i++) {
            delay = delay.multipliedBy(2);
        }
        return delay.compareTo(config.maxDelay()) > 0 ? config.maxDelay() : delay;
    }
}
