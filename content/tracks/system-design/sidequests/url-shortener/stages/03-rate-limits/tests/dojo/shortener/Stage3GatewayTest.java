package dojo.shortener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.shortener.Fakes.ManualClock;
import dojo.shortener.Fakes.MemoryAllocator;
import dojo.shortener.Fakes.MemoryStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class Stage3GatewayTest {
    // 5 writes at once, then one every 10 s; 20 reads at once, then one every second.
    private static final Limits LIMITS = new Limits(5, Duration.ofSeconds(10), 20, Duration.ofSeconds(1));

    private final ManualClock clock = new ManualClock();
    private final MemoryStore store = new MemoryStore();
    private final Shortener shortener = new Shortener(
            store, new MemoryAllocator(1), clock,
            new ShortenerConfig(100, 6, 1000, Duration.ofMinutes(10), Duration.ofMinutes(1)));
    private final Gateway gateway = new Gateway(shortener, clock, LIMITS);

    private static void assertStatus(int expected, Response response) {
        assertStatus(expected, response, "");
    }

    private static void assertStatus(int expected, Response response, String why) {
        assertEquals(expected, response.status(), () -> why + " (response " + response + ")");
    }

    @Test
    void postCreatesALinkAndGetRedirects() {
        Response created = gateway.post("ana", "https://example.com/page", null);
        assertEquals(new Response(201, "000001", Map.of("Location", "/000001")), created);
        Response redirect = gateway.get("bia", "000001");
        assertEquals(new Response(302, "", Map.of("Location", "https://example.com/page")), redirect);
    }

    @Test
    void aliasesAndErrorsHaveTheirStatus() {
        assertEquals(new Response(201, "promo", Map.of("Location", "/promo")),
                gateway.post("ana", "https://shop.example/", "promo"));
        assertStatus(409, gateway.post("ana", "https://evil.example/", "promo"));
        Response bad = gateway.post("ana", "ftp://example.com/", null);
        assertStatus(400, bad);
        assertTrue(!bad.body().isBlank(), "a 400 says what is wrong");
        assertStatus(400, gateway.post("ana", "https://example.com/", "no"));
        assertEquals(404, gateway.get("ana", "missing").status());
        assertEquals("", gateway.get("ana", "missing").body());
    }

    @Test
    void aBurstIsAllowedAndThenRefused() {
        for (int i = 0; i < 5; i++) {
            assertStatus(201, gateway.post("ana", "https://example.com/" + i, null));
        }
        Response refused = gateway.post("ana", "https://example.com/6", null);
        assertEquals(new Response(429, "", Map.of("Retry-After", "10")), refused);
        assertEquals(5, store.links.size(), "a refused request creates nothing");
    }

    @Test
    void tokensComeBackContinuouslyAndRetryAfterRoundsUp() {
        for (int i = 0; i < 5; i++) {
            gateway.post("ana", "https://example.com/" + i, null);
        }
        clock.advance(Duration.ofMillis(2_500));
        assertEquals("8", gateway.post("ana", "https://example.com/x", null).headers().get("Retry-After"),
                "7.5 s left, rounded up");
        clock.advance(Duration.ofMillis(7_499));
        assertEquals("1", gateway.post("ana", "https://example.com/x", null).headers().get("Retry-After"),
                "1 ms left is still at least 1 s");
        clock.advance(Duration.ofMillis(1));
        assertStatus(201, gateway.post("ana", "https://example.com/x", null));
        assertStatus(429, gateway.post("ana", "https://example.com/y", null));
        clock.advance(Duration.ofSeconds(25));
        assertStatus(201, gateway.post("ana", "https://example.com/y", null));
        assertStatus(201, gateway.post("ana", "https://example.com/z", null));
        assertStatus(429, gateway.post("ana", "https://example.com/w", null), "2.5 tokens give 2 requests");
    }

    @Test
    void theBucketNeverHoldsMoreThanTheBurst() {
        clock.advance(Duration.ofDays(1));
        gateway.post("ana", "https://example.com/first", null); // a new client starts full
        clock.advance(Duration.ofDays(1));
        int created = 0;
        for (int i = 0; i < 10; i++) {
            if (gateway.post("ana", "https://example.com/" + i, null).status() == 201) {
                created++;
            }
        }
        assertEquals(5, created);
    }

    @Test
    void badRequestsCostTokensToo() {
        for (int i = 0; i < 5; i++) {
            assertStatus(400, gateway.post("ana", "nonsense " + i, null));
        }
        assertStatus(429, gateway.post("ana", "https://example.com/", null));
        for (int i = 0; i < 20; i++) {
            assertStatus(404, gateway.get("bot", "scan" + i));
        }
        assertStatus(429, gateway.get("bot", "scan-more"));
    }

    @Test
    void readsAndWritesAndClientsHaveSeparateBuckets() {
        gateway.post("ana", "https://example.com/", "home");
        for (int i = 0; i < 4; i++) {
            gateway.post("ana", "https://example.com/" + i, null);
        }
        assertStatus(429, gateway.post("ana", "https://example.com/more", null));
        for (int i = 0; i < 20; i++) {
            assertStatus(302, gateway.get("ana", "home"));
        }
        assertStatus(429, gateway.get("ana", "home"));
        assertEquals("1", gateway.get("ana", "home").headers().get("Retry-After"));
        assertStatus(201, gateway.post("bia", "https://example.com/bia", null));
        assertStatus(302, gateway.get("bia", "home"));
    }

    @Test
    void fullClientsAreForgottenAndOthersAreNot() {
        for (int i = 0; i < 1_000; i++) {
            gateway.get("visitor-" + i, "x");
        }
        assertEquals(1_000, gateway.trackedClients());
        clock.advance(Duration.ofSeconds(50)); // enough to refill any bucket from empty
        for (int i = 0; i < 5; i++) {
            gateway.post("ana", "https://example.com/" + i, null);
        }
        assertEquals(1, gateway.trackedClients(), "the visitors are full again; only ana is kept");
        clock.advance(Duration.ofSeconds(50).minusMillis(1));
        gateway.get("bia", "x");
        assertTrue(gateway.trackedClients() <= 2);
        clock.advance(Duration.ofSeconds(25));
        gateway.get("caio", "x");
        assertEquals(5, created("ana", 8), "ana was full, forgotten or not, and gets exactly her burst");
    }

    @Test
    void aClientThatIsNotFullIsNeverForgotten() {
        clock.advance(Duration.ofSeconds(49));
        for (int i = 0; i < 5; i++) {
            gateway.post("ana", "https://example.com/" + i, null);
        }
        clock.advance(Duration.ofSeconds(1)); // 50 s since the gateway started: time to forget the full ones
        gateway.get("bia", "x");
        assertEquals(2, gateway.trackedClients(), "ana has 0.1 tokens: she must be kept");
        assertStatus(429, gateway.post("ana", "https://example.com/more", null));
    }

    private int created(String client, int posts) {
        int created = 0;
        for (int i = 0; i < posts; i++) {
            if (gateway.post(client, "https://example.com/s" + i, null).status() == 201) {
                created++;
            }
        }
        return created;
    }

    @Test
    void manyThreadsNeverGetMoreThanTheTokens() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger created = new AtomicInteger();
            List<Future<?>> done = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                done.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < 50; i++) {
                        if (gateway.post("ana", "https://example.com/" + i, null).status() == 201) {
                            created.incrementAndGet();
                        }
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : done) {
                f.get(30, TimeUnit.SECONDS);
            }
            assertEquals(5, created.get());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void manyThreadsCreateLinksWithCodesOfTheirOwn() throws Exception {
        var limits = new Limits(1_000, Duration.ofSeconds(1), 1_000, Duration.ofSeconds(1));
        var busy = new Gateway(shortener, clock, limits);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Set<String> codes = ConcurrentHashMap.newKeySet();
            List<Future<?>> done = new ArrayList<>();
            for (int t = 0; t < 8; t++) {
                String client = "client-" + t;
                done.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < 250; i++) {
                        Response r = busy.post(client, "https://example.com/" + client + "/" + i, null);
                        assertEquals(201, r.status());
                        codes.add(r.body());
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> f : done) {
                f.get(30, TimeUnit.SECONDS);
            }
            assertEquals(2_000, codes.size());
            assertEquals(2_000, store.links.size());
            int i = 0;
            for (String code : codes) {
                assertEquals(302, busy.get("checker-" + (i++ % 8), code).status());
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
