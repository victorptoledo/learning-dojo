package dojo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RoutesGradingTest {
    private static final Duration LIMIT = Duration.ofSeconds(5);

    private static int[][] randomEdges(Random random, int n, int m, int maxWeight) {
        int[][] edges = new int[m][];
        for (int i = 0; i < m; i++) {
            edges[i] = new int[] {random.nextInt(n), random.nextInt(n), random.nextInt(maxWeight + 1)};
        }
        return edges;
    }

    /** Bellman-Ford: slow and simple. */
    private static long[] slowDistances(int n, int[][] edges, int source) {
        long[] d = new long[n];
        Arrays.fill(d, Long.MAX_VALUE);
        d[source] = 0;
        for (int round = 0; round < n; round++) {
            for (int[] e : edges) {
                if (d[e[0]] != Long.MAX_VALUE && d[e[0]] + e[2] < d[e[1]]) d[e[1]] = d[e[0]] + e[2];
            }
        }
        for (int i = 0; i < n; i++) if (d[i] == Long.MAX_VALUE) d[i] = -1;
        return d;
    }

    /** Prim on an adjacency matrix: O(n²), fine for small graphs. */
    private static long slowMst(int n, int[][] edges) {
        if (n <= 1) return 0;
        long[][] w = new long[n][n];
        for (long[] row : w) Arrays.fill(row, Long.MAX_VALUE);
        for (int[] e : edges) {
            w[e[0]][e[1]] = Math.min(w[e[0]][e[1]], e[2]);
            w[e[1]][e[0]] = Math.min(w[e[1]][e[0]], e[2]);
        }
        boolean[] in = new boolean[n];
        long[] best = new long[n];
        Arrays.fill(best, Long.MAX_VALUE);
        best[0] = 0;
        long total = 0;
        for (int step = 0; step < n; step++) {
            int pick = -1;
            for (int v = 0; v < n; v++) if (!in[v] && (pick < 0 || best[v] < best[pick])) pick = v;
            if (best[pick] == Long.MAX_VALUE) return -1;
            in[pick] = true;
            total += best[pick];
            for (int v = 0; v < n; v++) if (!in[v] && w[pick][v] < best[v]) best[v] = w[pick][v];
        }
        return total;
    }

    // ---- union-find ----

    @Test
    void union_find_matches_naive_labels() {
        var random = new Random(41);
        int n = 60;
        var sets = new UnionFind(n);
        int[] label = new int[n];
        for (int i = 0; i < n; i++) label[i] = i;
        for (int step = 0; step < 2_000; step++) {
            int a = random.nextInt(n);
            int b = random.nextInt(n);
            boolean expectedMerge = label[a] != label[b];
            assertEquals(expectedMerge, sets.union(a, b));
            if (expectedMerge) {
                int from = label[b];
                for (int i = 0; i < n; i++) if (label[i] == from) label[i] = label[a];
            }
            int c = random.nextInt(n);
            int d = random.nextInt(n);
            assertEquals(label[c] == label[d], sets.connected(c, d));
            int finalC = c;
            assertEquals(Arrays.stream(label).filter(l -> l == label[finalC]).count(), sets.size(c));
            assertEquals(Arrays.stream(label).distinct().count(), sets.components());
        }
        assertThrows(IndexOutOfBoundsException.class, () -> sets.find(n));
    }

    @Test
    void union_find_stays_fast_on_a_chain() {
        int n = 1_000_000;
        assertTimeoutPreemptively(LIMIT, () -> {
            var sets = new UnionFind(n);
            for (int i = 0; i + 1 < n; i++) sets.union(i, i + 1);
            assertEquals(1, sets.components());
            assertEquals(n, sets.size(0));
            for (int i = 0; i < n; i++) assertTrue(sets.connected(0, i));
        });
    }

    // ---- grid ----

    @Test
    void grid_distances() {
        assertEquals(1, Routes.gridDistance(new String[] {"SE"}));
        assertEquals(-1, Routes.gridDistance(new String[] {"S#E"}));
        assertEquals(6, Routes.gridDistance(new String[] {"S..", "##.", "E.."}));
        assertThrows(IllegalArgumentException.class, () -> Routes.gridDistance(new String[] {"S.."}));
        assertThrows(IllegalArgumentException.class, () -> Routes.gridDistance(new String[] {"..E"}));
    }

    @Test
    void a_large_maze_is_one_bfs() {
        int size = 2_000;
        var rows = new String[size];
        for (int r = 0; r < size; r++) {
            var sb = new StringBuilder();
            for (int c = 0; c < size; c++) {
                // walls on every odd row except one gap, alternating sides: a long snake
                boolean wall = r % 2 == 1 && (r % 4 == 1 ? c != size - 1 : c != 0);
                sb.append(wall ? '#' : '.');
            }
            rows[r] = sb.toString();
        }
        rows[0] = "S" + rows[0].substring(1);
        rows[size - 2] = rows[size - 2].substring(0, size - 1) + "E";
        int distance = assertTimeoutPreemptively(LIMIT, () -> Routes.gridDistance(rows));
        assertTrue(distance > size * size / 2 - size * 2, "the snake is long: " + distance);
    }

    // ---- shortest paths ----

    @Test
    void distances_and_paths_match_bellman_ford() {
        var random = new Random(42);
        for (int round = 0; round < 300; round++) {
            int n = 1 + random.nextInt(9);
            int[][] edges = randomEdges(random, n, random.nextInt(20), 9);
            int source = random.nextInt(n);
            long[] expected = slowDistances(n, edges, source);
            assertArrayEquals(expected, Routes.shortestDistances(n, edges, source));
            int target = random.nextInt(n);
            List<Integer> path = Routes.shortestPath(n, edges, source, target);
            if (expected[target] < 0) {
                assertEquals(List.of(), path);
            } else {
                assertEquals(source, path.getFirst());
                assertEquals(target, path.getLast());
                assertEquals(expected[target], cost(edges, path), "the path must be a shortest one: " + path);
            }
        }
        assertEquals(List.of(3), Routes.shortestPath(5, new int[0][], 3, 3));
        assertThrows(IllegalArgumentException.class, () -> Routes.shortestDistances(2, new int[][] {{0, 1, -1}}, 0));
    }

    /** The cheapest way along the path, or fails if a step has no edge. */
    private static long cost(int[][] edges, List<Integer> path) {
        long total = 0;
        for (int i = 0; i + 1 < path.size(); i++) {
            long best = Long.MAX_VALUE;
            for (int[] e : edges) {
                if (e[0] == path.get(i) && e[1] == path.get(i + 1)) best = Math.min(best, e[2]);
            }
            assertTrue(best != Long.MAX_VALUE, "no edge " + path.get(i) + " → " + path.get(i + 1));
            total += best;
        }
        return total;
    }

    @Test
    void dijkstra_handles_a_large_graph() {
        int n = 200_000;
        var random = new Random(43);
        int[][] edges = new int[1_000_000][];
        for (int i = 0; i + 1 < n; i++) edges[i] = new int[] {i, i + 1, 1_000}; // a slow road through everything
        for (int i = n - 1; i < edges.length; i++) {
            edges[i] = new int[] {random.nextInt(n), random.nextInt(n), 1 + random.nextInt(100_000)};
        }
        long[] distances = assertTimeoutPreemptively(LIMIT, () -> Routes.shortestDistances(n, edges, 0));
        assertEquals(0, distances[0]);
        for (long d : distances) assertTrue(d >= 0 && d <= 1_000L * (n - 1));
        List<Integer> path = assertTimeoutPreemptively(LIMIT, () -> Routes.shortestPath(n, edges, 0, n - 1));
        assertEquals(n - 1, path.getLast());
        assertFalse(path.isEmpty());
    }

    // ---- spanning trees ----

    @Test
    void spanning_trees_match_prim() {
        var random = new Random(44);
        for (int round = 0; round < 300; round++) {
            int n = 1 + random.nextInt(9);
            int[][] edges = randomEdges(random, n, random.nextInt(25), 20);
            int[][] copy = Arrays.stream(edges).map(int[]::clone).toArray(int[][]::new);
            assertEquals(slowMst(n, edges), Routes.minimumSpanningTree(n, edges));
            assertTrue(Arrays.deepEquals(copy, edges), "the edges must not change");
        }
        assertEquals(0, Routes.minimumSpanningTree(1, new int[0][]));
        assertEquals(-1, Routes.minimumSpanningTree(2, new int[0][]));
    }

    @Test
    void kruskal_handles_a_large_graph() {
        int n = 200_000;
        var random = new Random(45);
        var list = new ArrayList<int[]>();
        for (int i = 1; i < n; i++) list.add(new int[] {i, random.nextInt(i), 1_000_000});
        for (int i = 0; i < 800_000; i++) list.add(new int[] {random.nextInt(n), random.nextInt(n), random.nextInt(1_000_000)});
        int[][] edges = list.toArray(int[][]::new);
        long total = assertTimeoutPreemptively(LIMIT, () -> Routes.minimumSpanningTree(n, edges));
        assertTrue(total > 0 && total < 1_000_000L * (n - 1), "total " + total);
    }
}
