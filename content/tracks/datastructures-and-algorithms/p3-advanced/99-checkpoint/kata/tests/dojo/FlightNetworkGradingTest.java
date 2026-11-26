package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class FlightNetworkGradingTest {
    private static final Duration LIMIT = Duration.ofSeconds(6);

    private record R(int from, int to, int price) {}

    /** A fixed-length code for an index: 0 → AAAAA, 1 → AAAAB, ... */
    private static String code(int index, int length) {
        char[] c = new char[length];
        for (int i = length - 1; i >= 0; i--) {
            c[i] = (char) ('A' + index % 26);
            index /= 26;
        }
        return new String(c);
    }

    // ---- slow references on small graphs ----

    private static boolean slowConnected(int n, List<R> routes, int a, int b) {
        boolean[] seen = new boolean[n];
        ArrayDeque<Integer> queue = new ArrayDeque<>(List.of(a));
        seen[a] = true;
        while (!queue.isEmpty()) {
            int x = queue.poll();
            for (R r : routes) {
                int y = r.from() == x ? r.to() : r.to() == x ? r.from() : -1;
                if (y >= 0 && !seen[y]) {
                    seen[y] = true;
                    queue.add(y);
                }
            }
        }
        return seen[b];
    }

    /** Cheapest walk with at most k routes, by trying every walk. */
    private static long slowWithin(List<R> routes, int at, int target, int k) {
        long best = at == target ? 0 : Long.MAX_VALUE;
        if (k == 0) return best;
        for (R r : routes) {
            if (r.from() != at) continue;
            long rest = slowWithin(routes, r.to(), target, k - 1);
            if (rest != Long.MAX_VALUE) best = Math.min(best, rest + r.price());
        }
        return best;
    }

    /** Prim's algorithm on a matrix of the cheapest price between each pair, either way. */
    private static long slowNetwork(int n, List<R> routes) {
        if (n < 2) return 0;
        long[][] w = new long[n][n];
        for (long[] row : w) Arrays.fill(row, Long.MAX_VALUE);
        for (R r : routes) {
            w[r.from()][r.to()] = Math.min(w[r.from()][r.to()], r.price());
            w[r.to()][r.from()] = Math.min(w[r.to()][r.from()], r.price());
        }
        boolean[] in = new boolean[n];
        long[] key = new long[n];
        Arrays.fill(key, Long.MAX_VALUE);
        key[0] = 0;
        long total = 0;
        for (int round = 0; round < n; round++) {
            int pick = -1;
            for (int v = 0; v < n; v++) if (!in[v] && (pick < 0 || key[v] < key[pick])) pick = v;
            if (key[pick] == Long.MAX_VALUE) return -1;
            in[pick] = true;
            total += key[pick];
            for (int v = 0; v < n; v++) if (!in[v] && w[pick][v] < key[v]) key[v] = w[pick][v];
        }
        return total;
    }

    @Test
    void every_answer_matches_slow_references_on_random_networks() {
        Random random = new Random(31);
        for (int round = 0; round < 300; round++) {
            int n = 2 + random.nextInt(7);
            List<String> names = new ArrayList<>();
            for (int i = 0; i < n; i++) names.add(code(random.nextInt(26 * 26 * 26), 1 + random.nextInt(3)));
            names = new ArrayList<>(new java.util.LinkedHashSet<>(names));
            n = names.size();
            var net = new FlightNetwork();
            List<R> routes = new ArrayList<>();
            for (String name : names) net.addAirport(name);
            int edges = random.nextInt(2 * n + 1);
            for (int e = 0; e < edges; e++) {
                int a = random.nextInt(n);
                int b = random.nextInt(n);
                if (a == b) continue;
                int price = random.nextInt(4) == 0 ? 0 : random.nextInt(30);
                net.addRoute(names.get(a), names.get(b), price);
                routes.add(new R(a, b, price));
                int x = random.nextInt(n);
                int y = random.nextInt(n);
                assertEquals(slowConnected(n, routes, x, y), net.connected(names.get(x), names.get(y)),
                        "connected " + names.get(x) + " " + names.get(y) + " after " + routes);
            }
            for (int q = 0; q < 6; q++) {
                int a = random.nextInt(n);
                int b = random.nextInt(n);
                long full = slowWithin(routes, a, b, n);
                assertEquals(full == Long.MAX_VALUE ? -1 : full, net.cheapest(names.get(a), names.get(b)),
                        "cheapest " + names.get(a) + " → " + names.get(b) + " in " + routes);
                int k = random.nextInt(4);
                long within = slowWithin(routes, a, b, k);
                assertEquals(within == Long.MAX_VALUE ? -1 : within, net.cheapestWithin(names.get(a), names.get(b), k),
                        "cheapestWithin " + k + " " + names.get(a) + " → " + names.get(b) + " in " + routes);
            }
            assertEquals(slowNetwork(n, routes), net.networkCost(), "networkCost of " + names + " " + routes);
            int[] degree = new int[n];
            for (R r : routes) {
                degree[r.from()]++;
                degree[r.to()]++;
            }
            for (String prefix : List.of("", names.get(0).substring(0, 1), names.get(0))) {
                int limit = random.nextInt(4);
                List<Integer> expected = new ArrayList<>();
                for (int i = 0; i < n; i++) if (names.get(i).startsWith(prefix)) expected.add(i);
                final List<String> fixed = names;
                expected.sort(Comparator.<Integer>comparingInt(i -> -degree[i]).thenComparing(fixed::get));
                List<String> want = new ArrayList<>();
                for (int i = 0; i < Math.min(limit, expected.size()); i++) want.add(names.get(expected.get(i)));
                assertEquals(want, net.suggest(prefix, limit), "suggest '" + prefix + "' " + limit);
            }
        }
    }

    @Test
    void the_flight_limit_counts_flights_not_the_cheapest_path_found_first() {
        var net = new FlightNetwork();
        net.addRoute("A", "B", 1);
        net.addRoute("B", "C", 1);
        net.addRoute("C", "D", 1);
        net.addRoute("A", "C", 5);
        // The cheapest way to C has two flights; within two flights to D, C must be reached in one.
        assertEquals(3, net.cheapest("A", "D"));
        assertEquals(6, net.cheapestWithin("A", "D", 2));
        assertEquals(3, net.cheapestWithin("A", "D", 3));
        assertEquals(-1, net.cheapestWithin("A", "D", 1));
        assertEquals(0, net.cheapestWithin("A", "A", 0));
        assertEquals(-1, net.cheapestWithin("A", "B", 0));
    }

    @Test
    void one_round_adds_at_most_one_flight_whatever_the_order_of_the_routes() {
        var net = new FlightNetwork();
        net.addRoute("A", "B", 1);
        net.addRoute("B", "C", 1);
        net.addRoute("C", "D", 1);
        net.addRoute("D", "E", 1);
        net.addRoute("A", "E", 100);
        assertEquals(100, net.cheapestWithin("A", "E", 1));
        assertEquals(100, net.cheapestWithin("A", "E", 2));
        assertEquals(100, net.cheapestWithin("A", "E", 3));
        assertEquals(4, net.cheapestWithin("A", "E", 4));
        assertEquals(4, net.cheapestWithin("A", "E", Integer.MAX_VALUE));
    }

    @Test
    void parallel_routes_zero_prices_and_one_way_routes() {
        var net = new FlightNetwork();
        net.addRoute("A", "B", 9);
        net.addRoute("A", "B", 4);
        net.addRoute("B", "C", 0);
        net.addRoute("C", "A", 0);
        assertEquals(4, net.cheapest("A", "C"));
        assertEquals(0, net.cheapest("B", "A"));
        assertEquals(0, net.cheapest("C", "C"));
        assertEquals(0, net.networkCost());
        assertEquals(List.of("A", "B", "C"), net.suggest("", 10));
        assertEquals(List.of(), net.suggest("Z", 3));
        assertEquals(List.of(), net.suggest("A", 0));
    }

    @Test
    void airports_and_networks_of_one_or_none() {
        var net = new FlightNetwork();
        assertEquals(0, net.networkCost());
        net.addAirport("SOLO");
        net.addAirport("SOLO");
        assertEquals(0, net.networkCost());
        assertTrue(net.connected("SOLO", "SOLO"));
        assertEquals(0, net.cheapest("SOLO", "SOLO"));
        assertEquals(List.of("SOLO"), net.suggest("SO", 2));
        net.addAirport("DUO");
        assertEquals(-1, net.networkCost());
        assertFalse(net.connected("SOLO", "DUO"));
    }

    @Test
    void ties_in_suggestions_go_by_code() {
        var net = new FlightNetwork();
        net.addRoute("CAB", "XX", 1);
        net.addRoute("CAA", "XX", 1);
        net.addRoute("CA", "CAC", 1);
        net.addRoute("CAC", "YY", 1);
        // CAC has 2 routes; CA, CAA and CAB have 1 each
        assertEquals(List.of("CAC", "CA", "CAA"), net.suggest("CA", 3));
        assertEquals(List.of("CAC", "CA", "CAA", "CAB"), net.suggest("C", 10));
        assertEquals(List.of("CAA"), net.suggest("CAA", 3));
        assertEquals(List.of("CAC", "XX"), net.suggest("", 2));
    }

    @Test
    void bad_input_is_refused() {
        var net = new FlightNetwork();
        net.addRoute("LIS", "MAD", 1);
        for (String bad : new String[] {null, "", "lis", "L1S", "ABCDEFGHI", "LÍS", " LIS"}) {
            assertThrows(IllegalArgumentException.class, () -> net.addAirport(bad), "code " + bad);
            assertThrows(IllegalArgumentException.class, () -> net.addRoute(bad, "LIS", 1), "code " + bad);
        }
        assertThrows(IllegalArgumentException.class, () -> net.addRoute("MAD", "MAD", 1));
        assertThrows(IllegalArgumentException.class, () -> net.addRoute("MAD", "LIS", -5));
        assertThrows(IllegalArgumentException.class, () -> net.connected("LIS", "OPO"));
        assertThrows(IllegalArgumentException.class, () -> net.cheapest("OPO", "LIS"));
        assertThrows(IllegalArgumentException.class, () -> net.cheapestWithin("LIS", "OPO", 2));
        assertThrows(IllegalArgumentException.class, () -> net.cheapestWithin("LIS", "MAD", -1));
        assertThrows(IllegalArgumentException.class, () -> net.suggest("l", 3));
        assertThrows(IllegalArgumentException.class, () -> net.suggest(null, 3));
        assertThrows(IllegalArgumentException.class, () -> net.suggest("L", -1));
        // a refused route leaves nothing behind
        assertThrows(IllegalArgumentException.class, () -> net.addRoute("OPO", "OPO", 1));
        assertThrows(IllegalArgumentException.class, () -> net.addRoute("OPO", "FAO", -1));
        assertThrows(IllegalArgumentException.class, () -> net.cheapest("OPO", "LIS"));
        assertEquals(1, net.networkCost());
    }

    // ---- scale ----

    @Test
    void regions_answer_while_routes_arrive() {
        int n = 200_000;
        Random random = new Random(7);
        assertTimeoutPreemptively(LIMIT, () -> {
            var net = new FlightNetwork();
            for (int i = 0; i < n; i++) net.addAirport(code(i, 4));
            int together = 0;
            for (int i = 0; i < 200_000; i++) {
                // a long chain first (deep trees without union by size), then random routes
                int a = i < 100_000 ? i : random.nextInt(n);
                int b = i < 100_000 ? i + 1 : random.nextInt(n);
                if (a != b) net.addRoute(code(a, 4), code(b, 4), 1);
                if (net.connected(code(random.nextInt(n), 4), code(random.nextInt(n), 4))) together++;
            }
            assertTrue(together > 0);
            assertTrue(net.connected(code(0, 4), code(100_000, 4)));
        }, "connected must not search the graph: use union-find");
    }

    private static FlightNetwork bigNetwork(int n, int edges, long seed, List<R> keep) {
        Random random = new Random(seed);
        var net = new FlightNetwork();
        for (int i = 0; i < n; i++) net.addAirport(code(i, 5));
        for (int i = 0; i + 1 < n; i++) {
            net.addRoute(code(i, 5), code(i + 1, 5), 1000); // a backbone so everything is reachable
            if (keep != null) keep.add(new R(i, i + 1, 1000));
        }
        for (int e = 0; e < edges; e++) {
            int a = random.nextInt(n);
            int b = random.nextInt(n);
            if (a == b) continue;
            int price = 1 + random.nextInt(500);
            net.addRoute(code(a, 5), code(b, 5), price);
            if (keep != null) keep.add(new R(a, b, price));
        }
        return net;
    }

    @Test
    void cheapest_fares_on_a_large_network() {
        assertTimeoutPreemptively(LIMIT, () -> {
            var net = bigNetwork(50_000, 300_000, 11, null);
            Random random = new Random(12);
            for (int q = 0; q < 25; q++) {
                long price = net.cheapest(code(random.nextInt(50_000), 5), code(random.nextInt(50_000), 5));
                assertTrue(price >= 0);
            }
        }, "cheapest must be Dijkstra with a priority queue, not Bellman-Ford over every airport");
    }

    @Test
    void flight_limits_on_a_large_network() {
        assertTimeoutPreemptively(LIMIT, () -> {
            var net = bigNetwork(50_000, 300_000, 21, null);
            Random random = new Random(22);
            for (int q = 0; q < 8; q++) {
                String a = code(random.nextInt(50_000), 5);
                String b = code(random.nextInt(50_000), 5);
                long few = net.cheapestWithin(a, b, 3);
                long more = net.cheapestWithin(a, b, 8);
                assertTrue(more != -1 || few == -1);
                if (few != -1) assertTrue(more <= few);
            }
        }, "cheapestWithin must cost O(flights · routes), not enumerate walks");
    }

    @Test
    void a_huge_flight_limit_on_a_long_chain() {
        assertTimeoutPreemptively(LIMIT, () -> {
            var net = new FlightNetwork();
            int n = 60_000;
            for (int i = 0; i + 1 < n; i++) net.addRoute(code(i, 4), code(i + 1, 4), 2);
            assertEquals(2L * (n - 1), net.cheapestWithin(code(0, 4), code(n - 1, 4), Integer.MAX_VALUE));
            assertEquals(2L * (n - 1), net.cheapestWithin(code(0, 4), code(n - 1, 4), n - 1));
            assertEquals(-1, net.cheapestWithin(code(0, 4), code(n - 1, 4), 50));
        }, "a cheapest route never needs more flights than airports minus one");
    }

    @Test
    void network_cost_on_a_large_network() {
        assertTimeoutPreemptively(LIMIT, () -> {
            var net = bigNetwork(60_000, 300_000, 31, null);
            long first = net.networkCost();
            assertTrue(first > 0);
            net.addRoute(code(0, 5), code(59_999, 5), 0);
            assertTrue(net.networkCost() <= first);
        }, "networkCost must be Kruskal (sort and union-find) or Prim with a heap");
    }

    @Test
    void network_cost_matches_prim_on_a_medium_network() {
        List<R> routes = new ArrayList<>();
        var net = bigNetwork(1_500, 6_000, 41, routes);
        assertEquals(slowNetwork(1_500, routes), net.networkCost());
    }

    @Test
    void suggestions_only_look_below_the_prefix() {
        assertTimeoutPreemptively(LIMIT, () -> {
            var net = new FlightNetwork();
            int n = 26 * 26 * 26 * 8; // 140,608 airports
            for (int i = 0; i < n; i++) net.addAirport(code(i * 3 + 1, 5));
            Random random = new Random(51);
            for (int i = 0; i < 60_000; i++) net.addRoute(code(random.nextInt(n) * 3 + 1, 5), code(random.nextInt(n) * 3 + 1, 5), 1);
            int found = 0;
            for (int q = 0; q < 60_000; q++) {
                // every code starts with A; the prefixes used here all exist
                String prefix = code(random.nextInt(620), 3);
                List<String> got = net.suggest(prefix, 5);
                for (String s : got) assertTrue(s.startsWith(prefix));
                found += got.size();
            }
            assertTrue(found > 0);
        }, "suggest must walk to the prefix and look only at the codes below it");
    }
}
