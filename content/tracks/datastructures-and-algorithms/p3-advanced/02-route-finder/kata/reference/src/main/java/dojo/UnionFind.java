package dojo;

/** Disjoint sets with path compression and union by size. */
public final class UnionFind {
    private final int[] parent;
    private final int[] size;
    private int components;

    public UnionFind(int n) {
        if (n < 0) throw new IllegalArgumentException("negative size");
        parent = new int[n];
        size = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }
        components = n;
    }

    public int find(int x) {
        int root = x;
        while (parent[root] != root) root = parent[root];
        while (parent[x] != root) { // second pass: point every node on the path at the root
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    public boolean union(int a, int b) {
        int ra = find(a);
        int rb = find(b);
        if (ra == rb) return false;
        if (size[ra] < size[rb]) {
            int t = ra;
            ra = rb;
            rb = t;
        }
        parent[rb] = ra;
        size[ra] += size[rb];
        components--;
        return true;
    }

    public boolean connected(int a, int b) {
        return find(a) == find(b);
    }

    public int size(int x) {
        return size[find(x)];
    }

    public int components() {
        return components;
    }
}
