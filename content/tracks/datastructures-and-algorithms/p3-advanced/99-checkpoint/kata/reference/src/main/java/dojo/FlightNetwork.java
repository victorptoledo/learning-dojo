package dojo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Airports joined by one-way priced routes: regions with union-find, the cheapest fare with
 * Dijkstra, the cheapest fare within a number of flights with a DP over flights, code
 * suggestions with a trie and the cheapest network with Kruskal.
 */
public final class FlightNetwork {
    private static final long UNREACHABLE = Long.MAX_VALUE;

    private record Route(int from, int to, int price) {}

    private static final class Node {
        final Node[] children = new Node[26];
        int airport = -1;
    }

    private final Map<String, Integer> ids = new HashMap<>();
    private final List<String> codes = new ArrayList<>();
    private final List<List<Route>> outgoing = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();
    private final Node root = new Node();
    private int[] parent = new int[16];
    private int[] size = new int[16];
    private int[] degree = new int[16];
    private int regions;

    /** Adds an airport without routes; adding a known airport does nothing. */
    public void addAirport(String code) {
        idOf(checkCode(code));
    }

    /** Adds a one-way route; both airports are added if they are new. */
    public void addRoute(String from, String to, int price) {
        checkCode(from);
        checkCode(to);
        if (from.equals(to)) throw new IllegalArgumentException("a route joins two different airports: " + from);
        if (price < 0) throw new IllegalArgumentException("negative price " + price);
        int a = idOf(from);
        int b = idOf(to);
        Route route = new Route(a, b, price);
        routes.add(route);
        outgoing.get(a).add(route);
        degree[a]++;
        degree[b]++;
        if (union(parent, size, a, b)) regions--;
    }

    /** Whether a chain of routes, in either direction, joins the two airports. */
    public boolean connected(String a, String b) {
        return find(parent, known(a)) == find(parent, known(b));
    }

    /** The cheapest total price from one airport to another, or -1 if there is no way. */
    public long cheapest(String from, String to) {
        int source = known(from);
        int target = known(to);
        long[] distance = new long[codes.size()];
        Arrays.fill(distance, UNREACHABLE);
        distance[source] = 0;
        PriorityQueue<long[]> queue = new PriorityQueue<>(Comparator.comparingLong(entry -> entry[0]));
        queue.add(new long[] {0, source});
        while (!queue.isEmpty()) {
            long[] entry = queue.poll();
            int node = (int) entry[1];
            if (entry[0] > distance[node]) continue;
            if (node == target) return entry[0];
            for (Route route : outgoing.get(node)) {
                long candidate = entry[0] + route.price();
                if (candidate < distance[route.to()]) {
                    distance[route.to()] = candidate;
                    queue.add(new long[] {candidate, route.to()});
                }
            }
        }
        return -1;
    }

    /** The cheapest total price using at most {@code maxFlights} routes, or -1. */
    public long cheapestWithin(String from, String to, int maxFlights) {
        int source = known(from);
        int target = known(to);
        if (maxFlights < 0) throw new IllegalArgumentException("negative flight limit " + maxFlights);
        // A cheapest route never repeats an airport, so it has at most n - 1 flights.
        if (maxFlights >= codes.size() - 1) return cheapest(from, to);
        long[] best = new long[codes.size()];
        Arrays.fill(best, UNREACHABLE);
        best[source] = 0;
        for (int flights = 1; flights <= maxFlights; flights++) {
            long[] next = best.clone(); // read only the previous round: one more flight per round
            boolean changed = false;
            for (Route route : routes) {
                long before = best[route.from()];
                if (before == UNREACHABLE) continue;
                long candidate = before + route.price();
                if (candidate < next[route.to()]) {
                    next[route.to()] = candidate;
                    changed = true;
                }
            }
            best = next;
            if (!changed) break;
        }
        return best[target] == UNREACHABLE ? -1 : best[target];
    }

    /**
     * Up to {@code limit} airports whose code starts with the prefix, the most routes first,
     * then by code.
     */
    public List<String> suggest(String prefix, int limit) {
        if (prefix == null || !letters(prefix)) throw new IllegalArgumentException("bad prefix " + prefix);
        if (limit < 0) throw new IllegalArgumentException("negative limit " + limit);
        Node node = root;
        for (int i = 0; i < prefix.length() && node != null; i++) node = node.children[prefix.charAt(i) - 'A'];
        if (node == null || limit == 0) return List.of();
        Comparator<Integer> bestFirst = Comparator.<Integer>comparingInt(id -> -degree[id]).thenComparing(codes::get);
        PriorityQueue<Integer> kept = new PriorityQueue<>(bestFirst.reversed()); // worst kept at the head
        ArrayDeque<Node> stack = new ArrayDeque<>();
        stack.push(node);
        while (!stack.isEmpty()) {
            Node current = stack.pop();
            if (current.airport >= 0) {
                kept.add(current.airport);
                if (kept.size() > limit) kept.poll();
            }
            for (Node child : current.children) if (child != null) stack.push(child);
        }
        List<Integer> ranked = new ArrayList<>(kept);
        ranked.sort(bestFirst);
        List<String> result = new ArrayList<>(ranked.size());
        for (int id : ranked) result.add(codes.get(id));
        return result;
    }

    /**
     * The lowest total price of a set of routes, each usable in both directions, that joins
     * every airport; 0 with fewer than two airports, -1 if the airports cannot all be joined.
     */
    public long networkCost() {
        if (regions > 1) return -1;
        List<Route> byPrice = new ArrayList<>(routes);
        byPrice.sort(Comparator.comparingInt(Route::price));
        int n = codes.size();
        int[] p = new int[n];
        int[] s = new int[n];
        for (int i = 0; i < n; i++) {
            p[i] = i;
            s[i] = 1;
        }
        long total = 0;
        int joined = 1;
        for (Route route : byPrice) {
            if (joined == n) break;
            if (union(p, s, route.from(), route.to())) {
                total += route.price();
                joined++;
            }
        }
        return total;
    }

    // ---- helpers ----

    private static boolean letters(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 'A' || c > 'Z') return false;
        }
        return text.length() <= 8;
    }

    private static String checkCode(String code) {
        if (code == null || code.isEmpty() || !letters(code)) throw new IllegalArgumentException("bad airport code " + code);
        return code;
    }

    private int known(String code) {
        Integer id = code == null ? null : ids.get(code);
        if (id == null) throw new IllegalArgumentException("unknown airport " + code);
        return id;
    }

    private int idOf(String code) {
        Integer existing = ids.get(code);
        if (existing != null) return existing;
        int id = codes.size();
        ids.put(code, id);
        codes.add(code);
        outgoing.add(new ArrayList<>());
        if (id == parent.length) {
            parent = Arrays.copyOf(parent, id * 2);
            size = Arrays.copyOf(size, id * 2);
            degree = Arrays.copyOf(degree, id * 2);
        }
        parent[id] = id;
        size[id] = 1;
        regions++;
        Node node = root;
        for (int i = 0; i < code.length(); i++) {
            int c = code.charAt(i) - 'A';
            if (node.children[c] == null) node.children[c] = new Node();
            node = node.children[c];
        }
        node.airport = id;
        return id;
    }

    private static int find(int[] parent, int x) {
        int root = x;
        while (parent[root] != root) root = parent[root];
        while (parent[x] != root) {
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    private static boolean union(int[] parent, int[] size, int a, int b) {
        int ra = find(parent, a);
        int rb = find(parent, b);
        if (ra == rb) return false;
        if (size[ra] < size[rb]) {
            int t = ra;
            ra = rb;
            rb = t;
        }
        parent[rb] = ra;
        size[ra] += size[rb];
        return true;
    }
}
