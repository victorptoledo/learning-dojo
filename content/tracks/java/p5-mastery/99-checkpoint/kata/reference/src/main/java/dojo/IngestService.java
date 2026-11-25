package dojo;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/** Takes order lines, enriches them, looks up the customer's name and stores them. */
public final class IngestService {
    private final CustomerDirectory directory;
    private final OrderSink sink;
    private final Logger log;
    private final List<Enricher> enrichers;
    private final LruCache<String, String> names;
    private final ExecutorService workers;
    private final AtomicBoolean accepting = new AtomicBoolean(true);
    private final LongAdder accepted = new LongAdder();
    private final LongAdder rejected = new LongAdder();
    private final LongAdder stored = new LongAdder();
    private final LongAdder failed = new LongAdder();

    public IngestService(Config config, CustomerDirectory directory, OrderSink sink, Logger log) {
        this.directory = directory;
        this.sink = sink;
        this.log = log;
        this.enrichers = Enrichers.named(config.enrichers());
        this.names = new LruCache<>(config.cacheSize());
        this.workers = Executors.newFixedThreadPool(config.workers());
        log.log(Level.INFO, "ingest started: {0}", config);
    }

    /** Accepts a line for processing; false when it is rejected or the service is stopping. */
    public boolean submit(String line) {
        if (!accepting.get()) {
            return false;
        }
        Order order;
        try {
            order = OrderParser.parse(line);
        } catch (BadOrderException e) {
            rejected.increment();
            log.log(Level.WARNING, "rejected an order line", e);
            return false;
        }
        accepted.increment();
        try {
            workers.execute(() -> process(order));
            return true;
        } catch (RejectedExecutionException e) {
            accepted.decrement();
            return false;
        }
    }

    private void process(Order order) {
        Order enriched = order;
        for (Enricher enricher : enrichers) {
            enriched = enricher.enrich(enriched);
        }
        String id = enriched.id();
        try {
            String name = names.get(enriched.customerId(), directory::nameOf);
            sink.store(enriched, name);
            stored.increment();
            log.log(Level.DEBUG, () -> "stored order " + id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // stopped by shutdown: the order is abandoned
        } catch (Exception e) {
            failed.increment();
            log.log(Level.WARNING, () -> "could not store order " + id, e);
        }
    }

    public Metrics metrics() {
        return new Metrics(accepted.sum(), rejected.sum(), stored.sum(), failed.sum(),
                names.hits(), names.misses(), names.size());
    }

    /**
     * Stops taking orders, lets the accepted ones finish for up to {@code grace}, then
     * interrupts what is left.
     */
    public ShutdownReport shutdown(Duration grace) {
        accepting.set(false);
        workers.shutdown();
        try {
            if (!workers.awaitTermination(grace.toNanos(), TimeUnit.NANOSECONDS)) {
                workers.shutdownNow();
                workers.awaitTermination(1, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
        long done = stored.sum();
        long broken = failed.sum();
        long abandoned = accepted.sum() - done - broken;
        log.log(Level.INFO, "ingest stopped: {0} stored, {1} failed, {2} abandoned", done, broken, abandoned);
        return new ShutdownReport(done, broken, abandoned);
    }
}
