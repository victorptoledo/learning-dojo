package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ParallelFetchGradingTest {
    private static Callable<String> slow(String value, long millis) {
        return () -> {
            Thread.sleep(millis);
            return value;
        };
    }

    @Test
    void keeps_the_order_of_the_calls() {
        var outcomes = ParallelFetch.fetchAll(
                List.of(slow("a", 300), slow("b", 10), slow("c", 150)), Duration.ofSeconds(5));
        assertEquals(List.of(
                new Outcome.Success<>("a"), new Outcome.Success<>("b"), new Outcome.Success<>("c")), outcomes);
    }

    @Test
    void runs_the_calls_at_the_same_time() {
        List<Callable<String>> calls = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            calls.add(slow("v" + i, 400));
        }
        long start = System.nanoTime();
        var outcomes = ParallelFetch.fetchAll(calls, Duration.ofSeconds(10));
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertEquals(200, ParallelFetch.successes(outcomes).size());
        assertTrue(millis < 4_000, "200 calls of 400 ms took " + millis + " ms: are they really in parallel?");
    }

    @Test
    void uses_virtual_threads() {
        Callable<Boolean> isVirtual = () -> Thread.currentThread().isVirtual();
        var outcomes = ParallelFetch.fetchAll(List.of(isVirtual, isVirtual), Duration.ofSeconds(5));
        assertEquals(List.of(new Outcome.Success<>(true), new Outcome.Success<>(true)), outcomes);
    }

    @Test
    void a_failing_call_becomes_a_failure_with_its_message() {
        Callable<String> boom = () -> {
            throw new IllegalStateException("service down");
        };
        Callable<String> silent = () -> {
            throw new IllegalStateException();
        };
        var outcomes = ParallelFetch.fetchAll(List.of(slow("a", 10), boom, silent), Duration.ofSeconds(5));
        assertEquals(List.of(
                new Outcome.Success<>("a"),
                new Outcome.Failure<>("service down"),
                new Outcome.Failure<>("IllegalStateException")), outcomes);
    }

    @Test
    void a_slow_call_times_out_without_holding_the_others() {
        var interrupted = new AtomicBoolean(false);
        Callable<String> stuck = () -> {
            try {
                Thread.sleep(10_000);
                return "late";
            } catch (InterruptedException e) {
                interrupted.set(true);
                throw e;
            }
        };
        long start = System.nanoTime();
        var outcomes = ParallelFetch.fetchAll(List.of(slow("a", 50), stuck, slow("c", 50)), Duration.ofMillis(800));
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertEquals(List.of(new Outcome.Success<>("a"), new Outcome.TimedOut<>(), new Outcome.Success<>("c")), outcomes);
        assertTrue(millis < 3_000, "fetchAll waited " + millis + " ms: cancel the calls that time out");
        assertTrue(interrupted.get(), "the timed-out call should be cancelled (interrupted)");
    }

    @Test
    void the_timeout_is_one_deadline_for_all_the_calls() {
        long start = System.nanoTime();
        var outcomes = ParallelFetch.fetchAll(
                List.of(slow("a", 600), slow("b", 1_200), slow("c", 1_200)), Duration.ofMillis(900));
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertEquals("1 ok, 0 failed, 2 timed out", ParallelFetch.summary(outcomes));
        assertTrue(millis < 2_000, "the deadline is shared: " + millis + " ms");
    }

    @Test
    void no_calls_no_outcomes() {
        assertEquals(List.of(), ParallelFetch.fetchAll(List.<Callable<String>>of(), Duration.ofSeconds(1)));
    }

    @Test
    void successes_and_summary_read_the_outcomes() {
        List<Outcome<Integer>> outcomes = List.of(
                new Outcome.Success<>(1), new Outcome.TimedOut<>(), new Outcome.Failure<>("x"), new Outcome.Success<>(3));
        assertEquals(List.of(1, 3), ParallelFetch.successes(outcomes));
        assertEquals("2 ok, 1 failed, 1 timed out", ParallelFetch.summary(outcomes));
        assertEquals("0 ok, 0 failed, 0 timed out", ParallelFetch.summary(List.of()));
    }
}
