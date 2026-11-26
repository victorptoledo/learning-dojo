package dojo;

import java.util.Objects;

/** Range queries over arrays: a difference array, a Fenwick tree, a sparse table and a segment tree. */
public final class RangeToolkit {
    private RangeToolkit() {}

    private static void checkRange(int lo, int hi, int n) {
        if (lo < 0 || hi > n || lo > hi) throw new IndexOutOfBoundsException("bad range [" + lo + ", " + hi + ") of " + n);
    }

    private static void checkNotEmpty(int lo, int hi) {
        if (lo == hi) throw new IllegalArgumentException("empty range at " + lo);
    }

    /** An array of n zeros after adding each update {lo, hi, delta} to every position of [lo, hi). */
    public static long[] applyRangeAdds(int n, int[][] updates) {
        if (n < 0) throw new IllegalArgumentException("negative size " + n);
        long[] diff = new long[n + 1];
        for (int[] u : updates) {
            if (u == null || u.length != 3) throw new IllegalArgumentException("an update is {lo, hi, delta}");
            checkRange(u[0], u[1], n);
            diff[u[0]] += u[2];
            diff[u[1]] -= u[2];
        }
        long[] values = new long[n];
        long running = 0;
        for (int i = 0; i < n; i++) {
            running += diff[i];
            values[i] = running;
        }
        return values;
    }

    /** Point updates and range sums in O(log n), and a search by prefix. */
    public static final class Fenwick {
        private final long[] tree; // 1-based
        private final long[] values;
        private int negatives;

        public Fenwick(long[] initial) {
            Objects.requireNonNull(initial);
            int n = initial.length;
            values = initial.clone();
            tree = new long[n + 1];
            for (int i = 0; i < n; i++) {
                tree[i + 1] += values[i];
                int parent = (i + 1) + ((i + 1) & -(i + 1));
                if (parent <= n) tree[parent] += tree[i + 1];
                if (values[i] < 0) negatives++;
            }
        }

        public int size() {
            return values.length;
        }

        public long get(int index) {
            Objects.checkIndex(index, values.length);
            return values[index];
        }

        public void add(int index, long delta) {
            set(index, get(index) + delta);
        }

        public void set(int index, long value) {
            Objects.checkIndex(index, values.length);
            long delta = value - values[index];
            if (values[index] < 0) negatives--;
            if (value < 0) negatives++;
            values[index] = value;
            for (int i = index + 1; i < tree.length; i += i & -i) tree[i] += delta;
        }

        /** The sum of the values in [lo, hi). */
        public long sum(int lo, int hi) {
            checkRange(lo, hi, values.length);
            return prefix(hi) - prefix(lo);
        }

        private long prefix(int count) {
            long sum = 0;
            for (int i = count; i > 0; i -= i & -i) sum += tree[i];
            return sum;
        }

        /**
         * The smallest position p such that the values of [0, p] add up to at least target, or -1
         * if the total is smaller. Needs a positive target and no negative values.
         */
        public int firstReaching(long target) {
            if (target <= 0) throw new IllegalArgumentException("target must be positive: " + target);
            if (negatives > 0) throw new IllegalStateException("a negative value breaks the search by prefix");
            int position = 0; // number of values known to add up to less than target
            long remaining = target;
            for (int step = Integer.highestOneBit(Math.max(1, values.length)); step > 0; step >>= 1) {
                int next = position + step;
                if (next < tree.length && tree[next] < remaining) {
                    position = next;
                    remaining -= tree[next];
                }
            }
            return position < values.length ? position : -1;
        }
    }

    /** Range minimum on a fixed array in O(1) per query. */
    public static final class SparseMin {
        private final long[][] table;
        private final int n;

        public SparseMin(long[] values) {
            n = values.length;
            int levels = n == 0 ? 1 : 32 - Integer.numberOfLeadingZeros(n);
            table = new long[levels][];
            table[0] = values.clone();
            for (int k = 1; k < levels; k++) {
                int width = 1 << k;
                table[k] = new long[n - width + 1];
                for (int i = 0; i + width <= n; i++) {
                    table[k][i] = Math.min(table[k - 1][i], table[k - 1][i + width / 2]);
                }
            }
        }

        /** The minimum of [lo, hi); an empty range throws IllegalArgumentException. */
        public long min(int lo, int hi) {
            checkRange(lo, hi, n);
            checkNotEmpty(lo, hi);
            int k = 31 - Integer.numberOfLeadingZeros(hi - lo);
            return Math.min(table[k][lo], table[k][hi - (1 << k)]);
        }
    }

    /** Point updates, range maximum and the first position of the maximum in O(log n). */
    public static final class MaxTree {
        private final int n;
        private final int[] best; // position of the maximum of each node, the leftmost on ties
        private final long[] values;

        public MaxTree(long[] initial) {
            n = initial.length;
            values = initial.clone();
            best = new int[2 * Math.max(1, n)];
            for (int i = 0; i < n; i++) best[n + i] = i;
            for (int i = n - 1; i >= 1; i--) best[i] = better(best[2 * i], best[2 * i + 1]);
        }

        private int better(int a, int b) {
            if (values[a] != values[b]) return values[a] > values[b] ? a : b;
            return Math.min(a, b);
        }

        public void set(int index, long value) {
            Objects.checkIndex(index, n);
            values[index] = value;
            for (int i = (index + n) / 2; i >= 1; i /= 2) best[i] = better(best[2 * i], best[2 * i + 1]);
        }

        public long max(int lo, int hi) {
            return values[argMax(lo, hi)];
        }

        /** The first position of the maximum of [lo, hi). */
        public int argMax(int lo, int hi) {
            checkRange(lo, hi, n);
            checkNotEmpty(lo, hi);
            int result = lo;
            for (int l = lo + n, h = hi + n; l < h; l /= 2, h /= 2) {
                if ((l & 1) == 1) result = better(result, best[l++]);
                if ((h & 1) == 1) result = better(result, best[--h]);
            }
            return result;
        }
    }
}
