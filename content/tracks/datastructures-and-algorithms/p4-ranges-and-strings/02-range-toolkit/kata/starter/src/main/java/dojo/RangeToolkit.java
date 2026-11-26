package dojo;

/** Range queries over arrays: a difference array, a Fenwick tree, a sparse table and a segment tree. */
public final class RangeToolkit {
    private RangeToolkit() {}

    /** An array of n zeros after adding each update {lo, hi, delta} to every position of [lo, hi). */
    public static long[] applyRangeAdds(int n, int[][] updates) {
        throw new UnsupportedOperationException("todo applyRangeAdds");
    }

    /** Point updates and range sums in O(log n), and a search by prefix. */
    public static final class Fenwick {
        public Fenwick(long[] initial) {
            throw new UnsupportedOperationException("todo Fenwick");
        }

        public int size() {
            throw new UnsupportedOperationException("todo size");
        }

        public long get(int index) {
            throw new UnsupportedOperationException("todo get");
        }

        public void add(int index, long delta) {
            throw new UnsupportedOperationException("todo add");
        }

        public void set(int index, long value) {
            throw new UnsupportedOperationException("todo set");
        }

        /** The sum of the values in [lo, hi). */
        public long sum(int lo, int hi) {
            throw new UnsupportedOperationException("todo sum");
        }

        /** The smallest position p such that the values of [0, p] add up to at least target, or -1. */
        public int firstReaching(long target) {
            throw new UnsupportedOperationException("todo firstReaching");
        }
    }

    /** Range minimum on a fixed array in O(1) per query. */
    public static final class SparseMin {
        public SparseMin(long[] values) {
            throw new UnsupportedOperationException("todo SparseMin");
        }

        /** The minimum of [lo, hi). */
        public long min(int lo, int hi) {
            throw new UnsupportedOperationException("todo min");
        }
    }

    /** Point updates, range maximum and the first position of the maximum in O(log n). */
    public static final class MaxTree {
        public MaxTree(long[] initial) {
            throw new UnsupportedOperationException("todo MaxTree");
        }

        public void set(int index, long value) {
            throw new UnsupportedOperationException("todo set");
        }

        /** The maximum of [lo, hi). */
        public long max(int lo, int hi) {
            throw new UnsupportedOperationException("todo max");
        }

        /** The first position of the maximum of [lo, hi). */
        public int argMax(int lo, int hi) {
            throw new UnsupportedOperationException("todo argMax");
        }
    }
}
