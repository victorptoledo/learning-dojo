package dojo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/** BFS on grids, Dijkstra on weighted graphs and Kruskal's minimum spanning tree. */
public final class Routes {
    private Routes() {}

    private static final int[][] STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public static int gridDistance(String[] grid) {
        int rows = grid.length;
        int start = -1;
        int end = -1;
        int cols = rows == 0 ? 0 : grid[0].length();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                char ch = grid[r].charAt(c);
                if (ch == 'S') start = r * cols + c;
                if (ch == 'E') end = r * cols + c;
            }
        }
        if (start < 0 || end < 0) throw new IllegalArgumentException("the grid needs an S and an E");
        int[] distance = new int[rows * cols];
        Arrays.fill(distance, -1);
        distance[start] = 0;
        int[] queue = new int[rows * cols];
        int head = 0;
        int tail = 0;
        queue[tail++] = start;
        while (head < tail) {
            int cell = queue[head++];
            if (cell == end) return distance[cell];
            int r = cell / cols;
            int c = cell % cols;
            for (int[] step : STEPS) {
                int nr = r + step[0];
                int nc = c + step[1];
                if (nr < 0 || nc < 0 || nr >= rows || nc >= cols || grid[nr].charAt(nc) == '#') continue;
                int next = nr * cols + nc;
                if (distance[next] >= 0) continue;
                distance[next] = distance[cell] + 1;
                queue[tail++] = next;
            }
        }
        return -1;
    }

    private record Edge(int to, int weight) {}

    private static List<List<Edge>> adjacency(int n, int[][] edges) {
        List<List<Edge>> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(new ArrayList<>());
        for (int[] e : edges) {
            if (e[2] < 0) throw new IllegalArgumentException("negative weight " + e[2]);
            out.get(e[0]).add(new Edge(e[1], e[2]));
        }
        return out;
    }

    /** Dijkstra: distances (-1 when unreachable) and the previous node on a shortest path. */
    private static long[][] dijkstra(int n, int[][] edges, int source) {
        var graph = adjacency(n, edges);
        long[] distance = new long[n];
        long[] previous = new long[n];
        Arrays.fill(distance, Long.MAX_VALUE);
        Arrays.fill(previous, -1);
        distance[source] = 0;
        PriorityQueue<long[]> queue = new PriorityQueue<>(Comparator.comparingLong(entry -> entry[0]));
        queue.add(new long[] {0, source});
        while (!queue.isEmpty()) {
            long[] entry = queue.poll();
            int node = (int) entry[1];
            if (entry[0] > distance[node]) continue; // an outdated entry
            for (Edge e : graph.get(node)) {
                long candidate = distance[node] + e.weight();
                if (candidate < distance[e.to()]) {
                    distance[e.to()] = candidate;
                    previous[e.to()] = node;
                    queue.add(new long[] {candidate, e.to()});
                }
            }
        }
        for (int i = 0; i < n; i++) if (distance[i] == Long.MAX_VALUE) distance[i] = -1;
        return new long[][] {distance, previous};
    }

    public static long[] shortestDistances(int n, int[][] edges, int source) {
        return dijkstra(n, edges, source)[0];
    }

    public static List<Integer> shortestPath(int n, int[][] edges, int source, int target) {
        long[][] result = dijkstra(n, edges, source);
        if (result[0][target] < 0) return List.of();
        var path = new ArrayDeque<Integer>();
        for (int node = target; node != -1; node = (int) result[1][node]) path.addFirst(node);
        return List.copyOf(path);
    }

    public static long minimumSpanningTree(int n, int[][] edges) {
        for (int[] e : edges) if (e[2] < 0) throw new IllegalArgumentException("negative weight " + e[2]);
        if (n <= 1) return 0;
        int[][] sorted = edges.clone();
        Arrays.sort(sorted, Comparator.comparingInt(e -> e[2]));
        var sets = new UnionFind(n);
        long total = 0;
        for (int[] e : sorted) {
            if (sets.union(e[0], e[1])) {
                total += e[2];
                if (sets.components() == 1) return total;
            }
        }
        return -1;
    }
}
