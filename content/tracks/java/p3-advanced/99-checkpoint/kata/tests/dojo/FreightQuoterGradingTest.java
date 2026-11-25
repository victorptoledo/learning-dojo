package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FreightQuoterGradingTest {
    @FunctionalInterface
    interface Behaviour {
        Quote answer(String name, Shipment shipment, int callNumber) throws Exception;
    }

    /** A fake carrier that records how it was called. */
    static final class FakeCarrier implements Carrier {
        final String name;
        final Behaviour behaviour;
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger running = new AtomicInteger();
        final AtomicInteger maxRunning = new AtomicInteger();
        final AtomicBoolean interrupted = new AtomicBoolean();
        final Set<String> problems = ConcurrentHashMap.newKeySet();

        FakeCarrier(String name, Behaviour behaviour) {
            this.name = name;
            this.behaviour = behaviour;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Quote quote(Shipment shipment) throws Exception {
            int now = running.incrementAndGet();
            maxRunning.accumulateAndGet(now, Math::max);
            try {
                if (!Thread.currentThread().isVirtual()) {
                    problems.add("called on a platform thread");
                }
                if (!FreightQuoter.ORDER_ID.isBound()) {
                    problems.add("ORDER_ID is not bound while the carrier runs");
                } else if (!FreightQuoter.ORDER_ID.get().equals(shipment.orderId())) {
                    problems.add("ORDER_ID is " + FreightQuoter.ORDER_ID.get() + " while quoting " + shipment.orderId());
                }
                return behaviour.answer(name, shipment, calls.incrementAndGet());
            } catch (InterruptedException e) {
                interrupted.set(true);
                throw e;
            } finally {
                running.decrementAndGet();
            }
        }
    }

    private static FakeCarrier priced(String name, long pricePerKg, int days, long millis) {
        return new FakeCarrier(name, (n, shipment, call) -> {
            if (millis > 0) {
                Thread.sleep(millis);
            }
            return new Quote(n, pricePerKg * shipment.weightKg(), days);
        });
    }

    private static FakeCarrier failing(String name, Exception error) {
        return new FakeCarrier(name, (n, shipment, call) -> {
            throw error;
        });
    }

    private static Shipment shipment(String id, int kg) {
        return new Shipment(id, "Lisbon", "Porto", kg);
    }

    private static long millisOf(Runnable action) {
        long start = System.nanoTime();
        action.run();
        return (System.nanoTime() - start) / 1_000_000;
    }

    @Test
    void results_follow_the_order_of_the_carriers() {
        var quoter = new FreightQuoter(List.of(
                priced("Rapido", 500, 1, 300), priced("Slowpost", 200, 6, 10), priced("Midway", 300, 3, 150)),
                4, Duration.ofSeconds(5));
        var board = quoter.quote(shipment("o-1", 2), Choice.cheapest());
        assertEquals(List.of(
                new QuoteResult.Quoted(new Quote("Rapido", 1000, 1)),
                new QuoteResult.Quoted(new Quote("Slowpost", 400, 6)),
                new QuoteResult.Quoted(new Quote("Midway", 600, 3))), board.results());
        assertEquals(Optional.of(new Quote("Slowpost", 400, 6)), board.best());
        assertEquals(shipment("o-1", 2), board.shipment());
    }

    @Test
    void asks_the_carriers_at_the_same_time_on_virtual_threads() {
        List<FakeCarrier> carriers = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            carriers.add(priced("C" + i, 100, 2, 400));
        }
        var quoter = new FreightQuoter(List.copyOf(carriers), 2, Duration.ofSeconds(5));
        long millis = millisOf(() -> quoter.quote(shipment("o-1", 1), Choice.cheapest()));
        assertTrue(millis < 1_200, "6 carriers of 400 ms took " + millis + " ms: are they asked at the same time?");
        for (FakeCarrier carrier : carriers) {
            assertEquals(Set.of(), carrier.problems);
        }
    }

    @Test
    void a_failing_carrier_becomes_failed_with_its_message() {
        var quoter = new FreightQuoter(List.of(
                priced("Rapido", 500, 1, 0),
                failing("Broken", new IllegalStateException("service down")),
                failing("Silent", new IllegalStateException()),
                new FakeCarrier("Empty", (n, s, c) -> null)),
                2, Duration.ofSeconds(5));
        var board = quoter.quote(shipment("o-1", 1), Choice.cheapest());
        assertEquals(List.of(
                new QuoteResult.Quoted(new Quote("Rapido", 500, 1)),
                new QuoteResult.Failed("Broken", "service down"),
                new QuoteResult.Failed("Silent", "IllegalStateException"),
                new QuoteResult.Failed("Empty", "no quote")), board.results());
        assertEquals(Optional.of(new Quote("Rapido", 500, 1)), board.best());
    }

    @Test
    void slow_carriers_time_out_within_one_shared_deadline_and_are_interrupted() {
        List<FakeCarrier> stuck = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            stuck.add(priced("Stuck" + i, 100, 1, 20_000));
        }
        List<Carrier> carriers = new ArrayList<>();
        carriers.add(priced("Rapido", 500, 1, 50));
        carriers.addAll(stuck);
        var quoter = new FreightQuoter(carriers, 2, Duration.ofMillis(300));
        long start = System.nanoTime();
        var board = quoter.quote(shipment("o-1", 1), Choice.cheapest());
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertEquals(List.of(
                new QuoteResult.Quoted(new Quote("Rapido", 500, 1)),
                new QuoteResult.TimedOut("Stuck1"),
                new QuoteResult.TimedOut("Stuck2"),
                new QuoteResult.TimedOut("Stuck3"),
                new QuoteResult.TimedOut("Stuck4")), board.results());
        assertTrue(millis < 1_000, "four stuck carriers with a 300 ms deadline took " + millis
                + " ms: is the deadline one for the whole quote, and are stuck calls cancelled?");
        long waitUntil = System.nanoTime() + 2_000_000_000L;
        while (stuck.stream().anyMatch(c -> !c.interrupted.get()) && System.nanoTime() < waitUntil) {
            Thread.onSpinWait();
        }
        assertTrue(stuck.stream().allMatch(c -> c.interrupted.get()),
                "a timed-out call must be cancelled with future.cancel(true)");
    }

    @Test
    void the_order_id_is_bound_for_the_carrier_and_only_during_the_call() {
        var carrier = priced("Rapido", 100, 1, 20);
        var quoter = new FreightQuoter(List.of(carrier), 8, Duration.ofSeconds(5));
        List<Shipment> shipments = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            shipments.add(shipment("o-" + i, 1));
        }
        quoter.quoteAll(shipments, Choice.cheapest());
        assertEquals(Set.of(), carrier.problems);
        assertFalse(FreightQuoter.ORDER_ID.isBound(), "ORDER_ID must not stay bound in the caller");
    }

    @Test
    void quote_all_keeps_the_order_of_the_shipments() {
        var quoter = new FreightQuoter(List.of(priced("Rapido", 100, 1, 30)), 16, Duration.ofSeconds(5));
        List<Shipment> shipments = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            shipments.add(shipment("o-" + i, i));
        }
        var boards = quoter.quoteAll(shipments, Choice.cheapest());
        assertEquals(40, boards.size());
        for (int i = 0; i < 40; i++) {
            assertEquals(shipments.get(i), boards.get(i).shipment());
            assertEquals(100L * (i + 1), boards.get(i).best().orElseThrow().priceCents());
        }
    }

    @Test
    void never_calls_a_carrier_more_than_the_limit_at_once_but_uses_the_whole_limit() {
        var rapido = priced("Rapido", 100, 1, 50);
        var slowpost = priced("Slowpost", 50, 5, 50);
        var quoter = new FreightQuoter(List.of(rapido, slowpost), 4, Duration.ofSeconds(10));
        List<Shipment> shipments = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            shipments.add(shipment("o-" + i, 1));
        }
        long millis = millisOf(() -> quoter.quoteAll(shipments, Choice.cheapest()));
        assertEquals(4, rapido.maxRunning.get(), "Rapido allows 4 calls at once");
        assertEquals(4, slowpost.maxRunning.get(), "Slowpost allows 4 calls at once");
        assertTrue(millis >= 450, "40 calls of 50 ms, 4 at a time, cannot take " + millis + " ms");
        assertTrue(millis < 1_600, "40 calls of 50 ms, 4 at a time, took " + millis + " ms: are shipments quoted at the same time?");
    }

    @Test
    void a_failure_gives_its_permit_back() {
        var broken = failing("Broken", new IllegalStateException("down"));
        var quoter = new FreightQuoter(List.of(broken), 1, Duration.ofMillis(500));
        for (int i = 0; i < 5; i++) {
            var board = quoter.quote(shipment("o-" + i, 1), Choice.cheapest());
            assertEquals(List.of(new QuoteResult.Failed("Broken", "down")), board.results(),
                    "call " + (i + 1) + ": a failed call must release its permit");
        }
    }

    @Test
    void a_timed_out_call_gives_its_permit_back() {
        var flaky = new FakeCarrier("Flaky", (n, s, call) -> {
            if (call == 1) {
                Thread.sleep(20_000);
            }
            return new Quote(n, 700, 2);
        });
        var quoter = new FreightQuoter(List.of(flaky), 1, Duration.ofMillis(300));
        assertEquals(List.of(new QuoteResult.TimedOut("Flaky")),
                quoter.quote(shipment("o-1", 1), Choice.cheapest()).results());
        assertEquals(List.of(new QuoteResult.Quoted(new Quote("Flaky", 700, 2))),
                quoter.quote(shipment("o-2", 1), Choice.cheapest()).results(),
                "the cancelled call must release its permit");
    }

    @Test
    void stats_count_every_call_without_losing_updates() {
        var a = priced("A", 1, 1, 0);
        var b = priced("B", 2, 2, 0);
        var broken = failing("Broken", new IllegalStateException("down"));
        var quoter = new FreightQuoter(List.of(a, b, broken), 64, Duration.ofSeconds(20));
        List<Shipment> shipments = new ArrayList<>();
        for (int i = 0; i < 2_000; i++) {
            shipments.add(shipment("o-" + i, 1));
        }
        quoter.quoteAll(shipments, Choice.cheapest());
        assertEquals(new Stats(Map.of("A", 2_000L, "B", 2_000L, "Broken", 2_000L), 2_000, 0), quoter.stats());
    }

    @Test
    void stats_count_timeouts_and_calls_actually_made() {
        var stuck = priced("Stuck", 1, 1, 20_000);
        var quoter = new FreightQuoter(List.of(stuck, priced("Rapido", 1, 1, 0)), 1, Duration.ofMillis(200));
        quoter.quote(shipment("o-1", 1), Choice.cheapest());
        Stats stats = quoter.stats();
        assertEquals(1, stats.timeouts());
        assertEquals(0, stats.failures());
        assertEquals(Map.of("Stuck", 1L, "Rapido", 1L), stats.calls());
    }

    @Test
    void choices_break_ties_as_documented() {
        var a = new Quote("Alpha", 1000, 3);
        var b = new Quote("Bravo", 1000, 2);
        var c = new Quote("Charlie", 800, 5);
        var d = new Quote("Delta", 1200, 2);
        var e = new Quote("Echo", 1000, 2);
        List<Quote> quotes = List.of(a, b, c, d, e);
        assertEquals(Optional.of(c), Choice.cheapest().pick(quotes));
        assertEquals(Optional.of(b), Choice.fastest().pick(quotes));
        assertEquals(Optional.of(b), Choice.cheapestWithin(3).pick(quotes));
        assertEquals(Optional.of(b), Choice.cheapest().pick(List.of(a, e, b)));
        assertEquals(Optional.empty(), Choice.cheapestWithin(1).pick(quotes));
        assertEquals(Optional.empty(), Choice.fastest().pick(List.of()));
    }

    @Test
    void the_best_quote_ignores_failures_and_a_custom_choice_works_as_a_lambda() {
        var quoter = new FreightQuoter(List.of(
                priced("Rapido", 500, 1, 0), failing("Broken", new IllegalStateException("down")),
                priced("Slowpost", 200, 6, 0)), 2, Duration.ofSeconds(5));
        Choice priciest = quotes -> quotes.stream().max(java.util.Comparator.comparingLong(Quote::priceCents));
        assertEquals("Rapido", quoter.quote(shipment("o-1", 1), priciest).best().orElseThrow().carrier());
        var allBroken = new FreightQuoter(List.of(failing("Broken", new IllegalStateException("down"))),
                1, Duration.ofSeconds(5));
        assertEquals(Optional.empty(), allBroken.quote(shipment("o-2", 1), Choice.cheapest()).best());
    }

    @Test
    void summary_counts_each_kind_of_result() {
        var board = new QuoteBoard(shipment("o-1", 1), List.of(
                new QuoteResult.Quoted(new Quote("Rapido", 1250, 2)),
                new QuoteResult.Failed("Broken", "down"),
                new QuoteResult.Quoted(new Quote("Slowpost", 900, 6)),
                new QuoteResult.TimedOut("Stuck")), Optional.of(new Quote("Rapido", 1250, 2)));
        assertEquals("2 quoted, 1 failed, 1 timed out; best: Rapido at 1250 cents in 2 days",
                FreightQuoter.summary(board));
        var empty = new QuoteBoard(shipment("o-2", 1), List.of(new QuoteResult.TimedOut("Stuck")), Optional.empty());
        assertEquals("0 quoted, 0 failed, 1 timed out; no quote", FreightQuoter.summary(empty));
    }

    @Test
    void the_constructor_refuses_bad_settings() {
        var one = List.<Carrier>of(priced("Rapido", 1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new FreightQuoter(List.of(), 1, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new FreightQuoter(one, 0, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new FreightQuoter(one, 1, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new FreightQuoter(one, 1, Duration.ofSeconds(-1)));
    }
}
