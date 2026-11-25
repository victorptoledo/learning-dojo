package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class IngestGradingTest {
    private static final String SECRET = "s3cret-Pa55";

    /** A System.Logger that keeps what is logged at enabled levels and notes eager messages at disabled ones. */
    static final class CapturingLogger implements System.Logger {
        record Entry(Level level, String message, Throwable thrown) {}

        final Level threshold;
        final List<Entry> entries = new CopyOnWriteArrayList<>();
        final List<String> eager = new CopyOnWriteArrayList<>();

        CapturingLogger(Level threshold) {
            this.threshold = threshold;
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public boolean isLoggable(Level level) {
            return level.getSeverity() >= threshold.getSeverity();
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
            if (!isLoggable(level)) {
                eager.add(level + " " + msg);
                return;
            }
            entries.add(new Entry(level, msg, thrown));
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String format, Object... params) {
            if (!isLoggable(level)) {
                if (params == null || params.length == 0) {
                    eager.add(level + " " + format);
                }
                return;
            }
            String message = params == null || params.length == 0 ? format : MessageFormat.format(format, params);
            entries.add(new Entry(level, message, null));
        }

        List<Entry> at(Level level) {
            return entries.stream().filter(e -> e.level() == level).toList();
        }
    }

    /** Remembers what was stored; can be slowed down or broken. */
    static final class RecordingSink implements OrderSink {
        final Map<String, Order> orders = new ConcurrentHashMap<>();
        final Map<String, String> names = new ConcurrentHashMap<>();
        final long millis;
        final AtomicInteger interrupted = new AtomicInteger();

        RecordingSink(long millis) {
            this.millis = millis;
        }

        @Override
        public void store(Order order, String customerName) throws Exception {
            if (order.sku().equals("BAD-0000")) {
                throw new IllegalStateException("database refused " + order.id());
            }
            if (millis > 0) {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    interrupted.incrementAndGet();
                    throw e;
                }
            }
            orders.put(order.id(), order);
            names.put(order.id(), customerName);
        }
    }

    static final class CountingDirectory implements CustomerDirectory {
        final AtomicInteger calls = new AtomicInteger();
        final String prefix;

        CountingDirectory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String nameOf(String customerId) {
            calls.incrementAndGet();
            return prefix + customerId;
        }
    }

    private static Map<String, String> env(String... pairs) {
        Map<String, String> env = new HashMap<>();
        env.put("DB_URL", "jdbc:postgresql://db:5432/orders");
        env.put("DB_PASSWORD", SECRET);
        for (int i = 0; i < pairs.length; i += 2) {
            if (pairs[i + 1] == null) {
                env.remove(pairs[i]);
            } else {
                env.put(pairs[i], pairs[i + 1]);
            }
        }
        return env;
    }

    private static String line(int n, String customer, int quantity, long price) {
        return "o-" + n + ";" + customer + ";ABC-" + String.format("%04d", n % 10_000) + ";" + quantity + ";" + price;
    }

    private static ConfigException configProblem(Map<String, String> env) {
        return assertThrows(ConfigException.class, () -> Config.fromEnv(env));
    }

    // ---- Incident 1: configuration ------------------------------------------------------

    @Test
    void config_uses_defaults_for_optional_settings() {
        Config config = Config.fromEnv(env());
        assertEquals(8080, config.port());
        assertEquals("jdbc:postgresql://db:5432/orders", config.dbUrl());
        assertEquals(SECRET, config.dbPassword());
        assertEquals(4, config.workers());
        assertEquals(1_000, config.cacheSize());
        assertEquals(List.of(), config.enrichers());
    }

    @Test
    void config_reports_every_problem_at_once_in_order() {
        ConfigException e = configProblem(env(
                "PORT", "abc", "DB_URL", null, "DB_PASSWORD", " ", "WORKERS", "0", "CACHE_SIZE", "-5",
                "ENRICHERS", "tax,gzip"));
        List<String> keys = e.problems().stream().map(p -> p.substring(0, p.indexOf(':'))).toList();
        assertEquals(List.of("PORT", "DB_URL", "DB_PASSWORD", "WORKERS", "CACHE_SIZE", "ENRICHERS"), keys,
                "one problem per bad key, each starting with \"KEY: \", in this order; got " + e.problems());
        assertTrue(e.problems().get(0).contains("abc"), "say which value was wrong: " + e.problems().get(0));
        assertTrue(e.problems().get(5).contains("gzip"), "say which enricher is unknown: " + e.problems().get(5));
    }

    @Test
    void config_checks_ranges_and_the_url_scheme() {
        ConfigException e = configProblem(env("PORT", "70000", "DB_URL", "postgres://db", "WORKERS", "65",
                "CACHE_SIZE", "0"));
        List<String> keys = e.problems().stream().map(p -> p.substring(0, p.indexOf(':'))).toList();
        assertEquals(List.of("PORT", "DB_URL", "WORKERS", "CACHE_SIZE"), keys, "got " + e.problems());
        Config edges = Config.fromEnv(env("PORT", "65535", "WORKERS", "64", "CACHE_SIZE", "1000000"));
        assertEquals(65_535, edges.port());
        assertEquals(64, edges.workers());
        assertEquals(1_000_000, edges.cacheSize());
    }

    @Test
    void config_trims_values_and_reads_the_enricher_list() {
        Config config = Config.fromEnv(env("PORT", " 9090 ", "ENRICHERS", " tax , bulk ,"));
        assertEquals(9090, config.port());
        assertEquals(List.of("tax", "bulk"), config.enrichers());
        assertEquals(List.of(), Config.fromEnv(env("ENRICHERS", "")).enrichers());
    }

    @Test
    void config_never_shows_the_password() {
        Config config = Config.fromEnv(env());
        String text = config.toString();
        assertFalse(text.contains(SECRET), "toString() shows the password: " + text);
        assertTrue(text.contains("jdbc:postgresql://db:5432/orders"), "keep the other settings readable: " + text);
        ConfigException e = configProblem(env("PORT", "abc"));
        assertFalse(e.getMessage().contains(SECRET), "the error shows the password: " + e.getMessage());
    }

    // ---- Incident 2: logging -------------------------------------------------------------

    @Test
    void logs_say_what_happened_without_the_password() {
        var log = new CapturingLogger(System.Logger.Level.DEBUG);
        var service = new IngestService(Config.fromEnv(env("WORKERS", "2")), new CountingDirectory("N-"),
                new RecordingSink(0), log);
        service.submit(line(1, "c1", 1, 100));
        service.submit("o-2;c1;BAD-0000;1;100");
        service.shutdown(Duration.ofSeconds(5));
        assertFalse(log.at(System.Logger.Level.INFO).isEmpty(), "log the start (and the stop) at INFO");
        for (var entry : log.entries) {
            assertFalse(entry.message().contains(SECRET), "a log line shows the password: " + entry.message());
        }
    }

    @Test
    void disabled_debug_messages_are_never_built() {
        var log = new CapturingLogger(System.Logger.Level.INFO);
        var service = new IngestService(Config.fromEnv(env()), new CountingDirectory("N-"), new RecordingSink(0), log);
        for (int i = 0; i < 50; i++) {
            service.submit(line(i, "c" + (i % 5), 1, 100));
        }
        service.shutdown(Duration.ofSeconds(5));
        assertEquals(List.of(), log.eager.stream().distinct().toList(),
                "messages built for a disabled level: use log(level, () -> ...) or log(level, format, params)");
    }

    @Test
    void failures_are_logged_once_at_warning_with_the_exception() {
        var log = new CapturingLogger(System.Logger.Level.INFO);
        var service = new IngestService(Config.fromEnv(env()), new CountingDirectory("N-"), new RecordingSink(0), log);
        assertFalse(service.submit("o-1;c1;abc;1;100"));
        assertTrue(service.submit("o-2;c1;BAD-0000;1;100"));
        service.shutdown(Duration.ofSeconds(5));
        var warnings = log.at(System.Logger.Level.WARNING);
        assertEquals(2, warnings.size(), "one WARNING per failure; got " + log.entries);
        assertTrue(warnings.stream().anyMatch(w -> w.thrown() instanceof BadOrderException),
                "attach the BadOrderException to its WARNING: " + warnings);
        assertTrue(warnings.stream().anyMatch(w -> w.thrown() instanceof IllegalStateException
                        && w.thrown().getMessage().equals("database refused o-2")),
                "attach the sink's exception to its WARNING: " + warnings);
        assertEquals(List.of(), log.at(System.Logger.Level.ERROR), "a bad line from a client is not an ERROR");
    }

    // ---- Incident 3: the cache -----------------------------------------------------------

    @Test
    void lru_cache_evicts_the_least_recently_used_entry() {
        var cache = new LruCache<String, String>(2);
        List<String> loads = new ArrayList<>();
        java.util.function.Function<String, String> loader = key -> {
            loads.add(key);
            return key.toUpperCase();
        };
        assertEquals("A", cache.get("a", loader));
        assertEquals("B", cache.get("b", loader));
        assertEquals("A", cache.get("a", loader));
        assertEquals("C", cache.get("c", loader));   // evicts b, the least recently used
        assertEquals("A", cache.get("a", loader));
        assertEquals("B", cache.get("b", loader));   // loaded again
        assertEquals(List.of("a", "b", "c", "b"), loads);
        assertEquals(2, cache.size());
        assertEquals(2, cache.hits());
        assertEquals(4, cache.misses());
        assertThrows(IllegalArgumentException.class, () -> new LruCache<String, String>(0));
    }

    @Test
    void the_service_caches_names_within_cache_size() {
        var directory = new CountingDirectory("N-");
        var service = new IngestService(Config.fromEnv(env("WORKERS", "1", "CACHE_SIZE", "10")), directory,
                new RecordingSink(0), new CapturingLogger(System.Logger.Level.INFO));
        for (int i = 0; i < 5; i++) {
            service.submit(line(i, "same", 1, 100));
        }
        for (int i = 0; i < 200; i++) {
            service.submit(line(100 + i, "c" + i, 1, 100));
        }
        service.shutdown(Duration.ofSeconds(10));
        Metrics metrics = service.metrics();
        assertEquals(201, directory.calls.get(), "one lookup per new customer");
        assertEquals(4, metrics.cacheHits());
        assertEquals(201, metrics.cacheMisses());
        assertEquals(10, metrics.cacheSize(), "the cache must stay within CACHE_SIZE");
    }

    @Test
    void two_services_do_not_share_a_cache() {
        var adaSink = new RecordingSink(0);
        var bobSink = new RecordingSink(0);
        var ada = new IngestService(Config.fromEnv(env("WORKERS", "1")), id -> "Ada", adaSink,
                new CapturingLogger(System.Logger.Level.INFO));
        ada.submit(line(1, "c-shared", 1, 100));
        ada.shutdown(Duration.ofSeconds(5));
        var bob = new IngestService(Config.fromEnv(env("WORKERS", "1")), id -> "Bob", bobSink,
                new CapturingLogger(System.Logger.Level.INFO));
        bob.submit(line(2, "c-shared", 1, 100));
        bob.shutdown(Duration.ofSeconds(5));
        assertEquals("Ada", adaSink.names.get("o-1"));
        assertEquals("Bob", bobSink.names.get("o-2"), "a static cache leaks between services (and never shrinks)");
    }

    // ---- Incident 4: the hot path --------------------------------------------------------

    @Test
    void parser_reads_valid_lines_and_refuses_bad_ones() {
        assertEquals(new Order("o-1", "c-7", "ABC-1234", 3, 1999), OrderParser.parse("o-1;c-7;ABC-1234;3;1999"));
        assertEquals(new Order("x", "y", "ZZZ-0000", 1000, 0), OrderParser.parse("x;y;ZZZ-0000;1000;0"));
        for (String bad : List.of("", "o-1;c-7;ABC-1234;3", "o-1;c-7;ABC-1234;3;1999;extra", ";c-7;ABC-1234;3;1",
                "o 1;c-7;ABC-1234;3;1", "o-1;;ABC-1234;3;1", "o-1;c-7;abc-1234;3;1", "o-1;c-7;ABC1234;3;1",
                "o-1;c-7;ABC-12345;3;1", "o-1;c-7;ABC-1234;0;1", "o-1;c-7;ABC-1234;1001;1",
                "o-1;c-7;ABC-1234;x;1", "o-1;c-7;ABC-1234;3;-1", "o-1;c-7;ABC-1234;3;10000001",
                "o-1;c-7;ABC-1234;3;")) {
            assertThrows(BadOrderException.class, () -> OrderParser.parse(bad), "should refuse: " + bad);
        }
        assertThrows(BadOrderException.class, () -> OrderParser.parse(null));
    }

    private static volatile Object blackhole;

    @Test
    void parsing_a_line_stays_within_its_allocation_budget() {
        var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        String[] lines = {"o-1001;c-7;ABC-1234;3;1999", "order-77;customer-12;XYZ-0042;12;250000",
                "o-9;c-123456;QQQ-9999;1;5", "o-31337;c-8;MNO-5150;999;9999999"};
        for (int i = 0; i < 50_000; i++) {
            blackhole = OrderParser.parse(lines[i & 3]);
        }
        int calls = 100_000;
        long before = threads.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < calls; i++) {
            blackhole = OrderParser.parse(lines[i & 3]);
        }
        long perCall = (threads.getCurrentThreadAllocatedBytes() - before) / calls;
        assertTrue(perCall <= BUDGET_BYTES, "parse allocates " + perCall + " bytes per line; the budget is "
                + BUDGET_BYTES + " (the Order and its three strings fit in it)");
    }

    private static final long BUDGET_BYTES = 256;

    // ---- Incident 5: shutting down -------------------------------------------------------

    @Test
    void shutdown_finishes_the_accepted_orders_and_then_refuses_new_ones() {
        var sink = new RecordingSink(20);
        var service = new IngestService(Config.fromEnv(env("WORKERS", "4")), new CountingDirectory("N-"), sink,
                new CapturingLogger(System.Logger.Level.INFO));
        for (int i = 0; i < 40; i++) {
            assertTrue(service.submit(line(i, "c" + (i % 3), 1, 100)));
        }
        ShutdownReport report = service.shutdown(Duration.ofSeconds(10));
        assertEquals(new ShutdownReport(40, 0, 0), report, "accepted orders must be drained, not dropped");
        assertEquals(40, sink.orders.size());
        assertFalse(service.submit(line(99, "c1", 1, 100)), "after shutdown, submit returns false");
        assertEquals(40, service.metrics().accepted());
    }

    @Test
    void shutdown_gives_up_after_the_grace_period_and_reports_what_is_abandoned() {
        var sink = new RecordingSink(60_000);
        var service = new IngestService(Config.fromEnv(env("WORKERS", "4")), new CountingDirectory("N-"), sink,
                new CapturingLogger(System.Logger.Level.INFO));
        for (int i = 0; i < 10; i++) {
            service.submit(line(i, "c1", 1, 100));
        }
        long start = System.nanoTime();
        ShutdownReport report = service.shutdown(Duration.ofMillis(300));
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertTrue(millis >= 250, "wait for the grace period before interrupting (" + millis + " ms)");
        assertTrue(millis < 3_000, "after the grace period, interrupt the workers (" + millis + " ms)");
        assertEquals(new ShutdownReport(0, 0, 10), report);
        assertEquals(4, sink.interrupted.get(), "the 4 running orders are interrupted");
    }

    @Test
    void metrics_count_each_outcome() {
        var service = new IngestService(Config.fromEnv(env("WORKERS", "2")), new CountingDirectory("N-"),
                new RecordingSink(0), new CapturingLogger(System.Logger.Level.INFO));
        service.submit(line(1, "c1", 1, 100));
        service.submit(line(2, "c1", 1, 100));
        service.submit("o-3;c1;BAD-0000;1;100");
        service.submit("not an order");
        service.shutdown(Duration.ofSeconds(5));
        Metrics metrics = service.metrics();
        assertEquals(3, metrics.accepted());
        assertEquals(1, metrics.rejected());
        assertEquals(2, metrics.stored());
        assertEquals(1, metrics.failed());
    }

    // ---- Incident 6: ready for a native image --------------------------------------------

    @Test
    void enrichers_come_from_an_explicit_registry() {
        List<Enricher> enrichers = Enrichers.named(List.of("tax", "bulk"));
        assertEquals(2, enrichers.size());
        assertInstanceOf(TaxEnricher.class, enrichers.get(0));
        assertInstanceOf(BulkEnricher.class, enrichers.get(1));
        assertThrows(IllegalArgumentException.class, () -> Enrichers.named(List.of("gzip")));
        assertThrows(IllegalArgumentException.class, () -> Enrichers.named(List.of("Tax")));
        assertSame(ConfigException.class, configProblem(env("ENRICHERS", "Tax")).getClass());
    }

    @Test
    void enrichers_run_in_the_configured_order() {
        var taxFirst = new RecordingSink(0);
        var service = new IngestService(Config.fromEnv(env("WORKERS", "1", "ENRICHERS", "tax,bulk")),
                new CountingDirectory("N-"), taxFirst, new CapturingLogger(System.Logger.Level.INFO));
        service.submit(line(1, "c1", 10, 1000));
        service.submit(line(2, "c1", 9, 1000));
        service.shutdown(Duration.ofSeconds(5));
        assertEquals(900, taxFirst.orders.get("o-1").priceCents(), "1000 + 20% = 1200, then 300 off = 900");
        assertEquals(1200, taxFirst.orders.get("o-2").priceCents());
        var bulkFirst = new RecordingSink(0);
        var other = new IngestService(Config.fromEnv(env("WORKERS", "1", "ENRICHERS", "bulk,tax")),
                new CountingDirectory("N-"), bulkFirst, new CapturingLogger(System.Logger.Level.INFO));
        other.submit(line(1, "c1", 10, 1000));
        other.shutdown(Duration.ofSeconds(5));
        assertEquals(840, bulkFirst.orders.get("o-1").priceCents(), "1000 - 300 = 700, then + 20% = 840");
    }

    @Test
    void the_code_uses_no_reflection() throws IOException {
        Path main = Path.of("src", "main", "java");
        assertTrue(Files.isDirectory(main), "run the tests from the kata folder");
        List<String> found = new ArrayList<>();
        int files;
        try (Stream<Path> paths = Files.walk(main)) {
            List<Path> sources = paths.filter(p -> p.toString().endsWith(".java")).toList();
            files = sources.size();
            for (Path source : sources) {
                String code = Files.readString(source);
                for (String banned : List.of("Class.forName", "newInstance(", "getDeclaredConstructor",
                        "getConstructor(", "java.lang.reflect", "ServiceLoader")) {
                    if (code.contains(banned)) {
                        found.add(main.relativize(source) + " uses " + banned);
                    }
                }
            }
        }
        assertTrue(files >= 10, "only " + files + " source files found under " + main.toAbsolutePath());
        Collections.sort(found);
        assertEquals(List.of(), found, "a native image only sees what is reachable at build time");
    }
}
