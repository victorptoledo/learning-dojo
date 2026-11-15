package dojo;

import java.util.List;

/** BFS on grids, Dijkstra on weighted graphs and Kruskal's minimum spanning tree. */
public final class Routes {
    private Routes() {}

    /** Fewest steps from S to E, or -1. */
    public static int gridDistance(String[] grid) {
        throw new UnsupportedOperationException("todo gridDistance");
    }

    /** Shortest directed distances from source; -1 where unreachable. Edges are {from, to, weight}. */
    public static long[] shortestDistances(int n, int[][] edges, int source) {
        throw new UnsupportedOperationException("todo shortestDistances from " + source);
    }

    /** The nodes of one shortest directed path, source first; empty when there is none. */
    public static List<Integer> shortestPath(int n, int[][] edges, int source, int target) {
        throw new UnsupportedOperationException("todo shortestPath " + source + " → " + target);
    }

    /** Total weight of a minimum spanning tree (undirected edges), or -1 if not connected. */
    public static long minimumSpanningTree(int n, int[][] edges) {
        throw new UnsupportedOperationException("todo minimumSpanningTree of " + n + " nodes");
    }
}
