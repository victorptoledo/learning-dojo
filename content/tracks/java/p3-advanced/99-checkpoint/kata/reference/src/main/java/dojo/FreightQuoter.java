package dojo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;

/** Asks every carrier for a quote at the same time and picks the best one. */
public final class FreightQuoter {
    /** The order being quoted, bound while a carrier is called, so the carrier can log it. */
    public static final ScopedValue<String> ORDER_ID = ScopedValue.newInstance();

    private final List<Carrier> carriers;
    private final Map<String, Semaphore> permits = new LinkedHashMap<>();
    private final Duration timeout;
    private final ConcurrentHashMap<String, LongAdder> calls = new ConcurrentHashMap<>();
    private final LongAdder failures = new LongAdder();
    private final LongAdder timeouts = new LongAdder();

    public FreightQuoter(List<Carrier> carriers, int maxCallsPerCarrier, Duration timeout) {
        if (carriers == null || carriers.isEmpty()) {
            throw new IllegalArgumentException("at least one carrier is required");
        }
        if (maxCallsPerCarrier < 1) {
            throw new IllegalArgumentException("maxCallsPerCarrier must be at least 1");
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.carriers = List.copyOf(carriers);
        this.timeout = timeout;
        for (Carrier carrier : this.carriers) {
            permits.put(carrier.name(), new Semaphore(maxCallsPerCarrier));
        }
    }

    /** One result per carrier, in carrier order, all asked at the same time within one deadline. */
    public QuoteBoard quote(Shipment shipment, Choice choice) {
        long deadline = System.nanoTime() + timeout.toNanos();
        List<QuoteResult> results = new ArrayList<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Quote>> futures = new ArrayList<>();
            for (Carrier carrier : carriers) {
                futures.add(executor.submit(() -> ScopedValue.where(ORDER_ID, shipment.orderId())
                        .call(() -> call(carrier, shipment))));
            }
            for (int i = 0; i < carriers.size(); i++) {
                results.add(await(carriers.get(i).name(), futures.get(i), deadline));
            }
        }
        List<Quote> quotes = results.stream()
                .<Quote>mapMulti((result, out) -> {
                    if (result instanceof QuoteResult.Quoted(Quote quote)) {
                        out.accept(quote);
                    }
                })
                .toList();
        return new QuoteBoard(shipment, results, choice.pick(quotes));
    }

    private Quote call(Carrier carrier, Shipment shipment) throws Exception {
        Semaphore limit = permits.get(carrier.name());
        limit.acquire();
        try {
            calls.computeIfAbsent(carrier.name(), name -> new LongAdder()).increment();
            return carrier.quote(shipment);
        } finally {
            limit.release();
        }
    }

    private QuoteResult await(String carrier, Future<Quote> future, long deadline) {
        try {
            long left = Math.max(0, deadline - System.nanoTime());
            Quote quote = future.get(left, TimeUnit.NANOSECONDS);
            if (quote == null) {
                failures.increment();
                return new QuoteResult.Failed(carrier, "no quote");
            }
            return new QuoteResult.Quoted(quote);
        } catch (TimeoutException e) {
            future.cancel(true);
            timeouts.increment();
            return new QuoteResult.TimedOut(carrier);
        } catch (ExecutionException e) {
            failures.increment();
            Throwable cause = e.getCause();
            String reason = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
            return new QuoteResult.Failed(carrier, reason);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            timeouts.increment();
            return new QuoteResult.TimedOut(carrier);
        }
    }

    /** Quotes every shipment at the same time; one board per shipment, in order. */
    public List<QuoteBoard> quoteAll(List<Shipment> shipments, Choice choice) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<QuoteBoard>> futures = shipments.stream()
                    .map(shipment -> executor.submit(() -> quote(shipment, choice)))
                    .toList();
            List<QuoteBoard> boards = new ArrayList<>();
            for (Future<QuoteBoard> future : futures) {
                boards.add(future.get());
            }
            return List.copyOf(boards);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while quoting", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("quoting failed", e.getCause());
        }
    }

    public Stats stats() {
        Map<String, Long> snapshot = new LinkedHashMap<>();
        calls.forEach((carrier, count) -> snapshot.put(carrier, count.sum()));
        return new Stats(snapshot, failures.sum(), timeouts.sum());
    }

    /** "2 quoted, 1 failed, 1 timed out; best: Rapido at 1250 cents in 2 days". */
    public static String summary(QuoteBoard board) {
        int quoted = 0;
        int failed = 0;
        int timedOut = 0;
        for (QuoteResult result : board.results()) {
            switch (result) {
                case QuoteResult.Quoted q -> quoted++;
                case QuoteResult.Failed f -> failed++;
                case QuoteResult.TimedOut t -> timedOut++;
            }
        }
        String best = board.best()
                .map(q -> "best: " + q.carrier() + " at " + q.priceCents() + " cents in " + q.days() + " days")
                .orElse("no quote");
        return quoted + " quoted, " + failed + " failed, " + timedOut + " timed out; " + best;
    }
}
