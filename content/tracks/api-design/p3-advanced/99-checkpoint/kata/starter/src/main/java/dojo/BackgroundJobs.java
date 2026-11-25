package dojo;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Work that happens after the request has been answered. Written for you. In a real service
 * a worker pool would pick jobs up; here the tests call {@link #runDue()} to let the
 * background work run, and move {@link Ticker} forward to make delayed jobs due.
 */
@Component
public class BackgroundJobs {
    private record Job(Instant due, long order, Runnable task) {}

    private final Ticker ticker;
    private final List<Job> jobs = new ArrayList<>();
    private final AtomicLong order = new AtomicLong();

    public BackgroundJobs(Ticker ticker) {
        this.ticker = ticker;
    }

    /** Runs the task as soon as the workers get to it. */
    public void submit(Runnable task) {
        schedule(Duration.ZERO, task);
    }

    /** Runs the task once this much time has passed on the {@link Ticker}. */
    public void schedule(Duration delay, Runnable task) {
        synchronized (jobs) {
            jobs.add(new Job(ticker.now().plus(delay), order.incrementAndGet(), task));
        }
    }

    /**
     * Runs every job that is due, earliest first, including the jobs they schedule that are
     * already due, and returns how many ran.
     */
    public int runDue() {
        int ran = 0;
        for (Optional<Job> next = takeDue(); next.isPresent(); next = takeDue()) {
            next.get().task().run();
            ran++;
        }
        return ran;
    }

    private Optional<Job> takeDue() {
        synchronized (jobs) {
            Optional<Job> next = jobs.stream()
                    .filter(job -> !job.due().isAfter(ticker.now()))
                    .min(Comparator.comparing(Job::due).thenComparingLong(Job::order));
            next.ifPresent(jobs::remove);
            return next;
        }
    }
}
