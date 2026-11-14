package dojo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Runs many slow calls at the same time, one virtual thread each. */
public final class ParallelFetch {
    private ParallelFetch() {}

    /** One outcome per call, in the same order as the calls. */
    public static <T> List<Outcome<T>> fetchAll(List<Callable<T>> calls, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        List<Outcome<T>> outcomes = new ArrayList<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<T>> futures = calls.stream().map(executor::submit).toList();
            for (Future<T> future : futures) {
                outcomes.add(await(future, deadline));
            }
        }
        return List.copyOf(outcomes);
    }

    private static <T> Outcome<T> await(Future<T> future, long deadline) {
        try {
            long left = Math.max(0, deadline - System.nanoTime());
            return new Outcome.Success<>(future.get(left, TimeUnit.NANOSECONDS));
        } catch (TimeoutException e) {
            future.cancel(true);
            return new Outcome.TimedOut<>();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            String message = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
            return new Outcome.Failure<>(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            return new Outcome.TimedOut<>();
        }
    }

    /** The values of the successful outcomes, in order. */
    public static <T> List<T> successes(List<Outcome<T>> outcomes) {
        List<T> values = new ArrayList<>();
        for (Outcome<T> outcome : outcomes) {
            if (outcome instanceof Outcome.Success<T>(T value)) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    /** "3 ok, 1 failed, 1 timed out". */
    public static String summary(List<? extends Outcome<?>> outcomes) {
        int ok = 0;
        int failed = 0;
        int timedOut = 0;
        for (Outcome<?> outcome : outcomes) {
            switch (outcome) {
                case Outcome.Success<?> s -> ok++;
                case Outcome.Failure<?> f -> failed++;
                case Outcome.TimedOut<?> t -> timedOut++;
            }
        }
        return ok + " ok, " + failed + " failed, " + timedOut + " timed out";
    }
}
