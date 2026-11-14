package dojo;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

/** Runs many slow calls at the same time, one virtual thread each. */
public final class ParallelFetch {
    private ParallelFetch() {}

    /** One outcome per call, in the same order as the calls. */
    public static <T> List<Outcome<T>> fetchAll(List<Callable<T>> calls, Duration timeout) {
        throw new UnsupportedOperationException("todo fetch " + calls.size() + " calls within " + timeout);
    }

    /** The values of the successful outcomes, in order. */
    public static <T> List<T> successes(List<Outcome<T>> outcomes) {
        throw new UnsupportedOperationException("todo successes of " + outcomes.size());
    }

    /** "3 ok, 1 failed, 1 timed out". */
    public static String summary(List<? extends Outcome<?>> outcomes) {
        throw new UnsupportedOperationException("todo summary of " + outcomes.size());
    }
}
