package dojo;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/** Takes order lines, enriches them, looks up the customer's name and stores them. */
public final class IngestService {
    private static final Map<String, String> CUSTOMER_NAMES = new HashMap<>();

    private final CustomerDirectory directory;
    private final OrderSink sink;
    private final Logger log;
    private final List<Enricher> enrichers;
    private final ExecutorService workers;
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong stored = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    public IngestService(Config config, CustomerDirectory directory, OrderSink sink, Logger log) {
        this.directory = directory;
        this.sink = sink;
        this.log = log;
        this.enrichers = Enrichers.named(config.enrichers());
        this.workers = Executors.newFixedThreadPool(config.workers());
        log.log(Level.INFO, "ingest started with " + config);
    }

    /** Accepts a line for processing; false when it is rejected. */
    public boolean submit(String line) {
        Order order;
        try {
            order = OrderParser.parse(line);
        } catch (BadOrderException e) {
            rejected.incrementAndGet();
            log.log(Level.ERROR, "bad line: " + e.getMessage());
            return false;
        }
        accepted.incrementAndGet();
        workers.execute(() -> process(order));
        return true;
    }

    private void process(Order order) {
        Order enriched = order;
        for (Enricher enricher : enrichers) {
            enriched = enricher.enrich(enriched);
        }
        try {
            String name;
            if (CUSTOMER_NAMES.containsKey(enriched.customerId())) {
                hits.incrementAndGet();
                name = CUSTOMER_NAMES.get(enriched.customerId());
            } else {
                misses.incrementAndGet();
                name = directory.nameOf(enriched.customerId());
                CUSTOMER_NAMES.put(enriched.customerId(), name);
            }
            sink.store(enriched, name);
            stored.incrementAndGet();
            log.log(Level.DEBUG, "stored order " + enriched);
        } catch (Exception e) {
            failed.incrementAndGet();
            e.printStackTrace();
        }
    }

    public Metrics metrics() {
        return new Metrics(accepted.get(), rejected.get(), stored.get(), failed.get(),
                hits.get(), misses.get(), CUSTOMER_NAMES.size());
    }

    /** Stops the service. */
    public ShutdownReport shutdown(Duration grace) {
        workers.shutdownNow();
        long abandoned = accepted.get() - stored.get() - failed.get();
        return new ShutdownReport(stored.get(), failed.get(), abandoned);
    }
}
