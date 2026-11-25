package dojo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import org.springframework.stereotype.Component;

/** The availability SLI of the accounts API over a sliding window, and its error budget. */
@Component
public class SloTracker {
    /** 99% of requests in the window must be good. */
    public static final double OBJECTIVE = 0.99;
    public static final Duration WINDOW = Duration.ofHours(1);
    /** Page a human when the budget burns at least this many times faster than the SLO allows. */
    public static final double PAGE_BURN_RATE = 14.4;

    private record Sample(Instant at, boolean good) {}

    private final Clock clock;
    private final Deque<Sample> samples = new ArrayDeque<>();

    public SloTracker(Clock clock) {
        this.clock = clock;
    }

    /** One finished request: good unless the server failed it. */
    public synchronized void record(boolean good) {
        samples.addLast(new Sample(clock.instant(), good));
    }

    public synchronized SloReport report() {
        Instant cutoff = clock.instant().minus(WINDOW);
        while (!samples.isEmpty() && !samples.peekFirst().at().isAfter(cutoff)) {
            samples.removeFirst();
        }
        long total = samples.size();
        if (total == 0) {
            return new SloReport(0, 0, 1.0, 0.0, 1.0, false);
        }
        long good = samples.stream().filter(Sample::good).count();
        double badRatio = (double) (total - good) / total;
        double burnRate = badRatio / (1 - OBJECTIVE);
        return new SloReport(total, good, (double) good / total, burnRate, 1 - burnRate, burnRate >= PAGE_BURN_RATE);
    }
}
