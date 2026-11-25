package dojo;

import java.time.Clock;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** The availability SLI of the accounts API over a sliding window, and its error budget. */
@Component
public class SloTracker {
    /** 99% of requests in the window must be good. */
    public static final double OBJECTIVE = 0.99;
    public static final Duration WINDOW = Duration.ofHours(1);
    /** Page a human when the budget burns at least this many times faster than the SLO allows. */
    public static final double PAGE_BURN_RATE = 14.4;

    private final Clock clock;

    public SloTracker(Clock clock) {
        this.clock = clock;
    }

    /** One finished request: good unless the server failed it. */
    public void record(boolean good) {
        throw new UnsupportedOperationException("todo record " + good);
    }

    public SloReport report() {
        throw new UnsupportedOperationException("todo report");
    }
}
