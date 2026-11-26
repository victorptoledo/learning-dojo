package dojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An editable log that counts watched patterns per line with KMP and answers range questions
 * with a Fenwick tree (sums, first line with a match) and a segment tree (busiest line).
 */
public final class LogIndex {
    private final List<String> lines = new ArrayList<>();
    private final Map<String, Watch> watches = new HashMap<>();
    private int capacity = 16;

    /** Counts of one pattern per line, with the two trees over them. */
    private final class Watch {
        final String pattern;
        final int[] pi;
        long[] count = new long[capacity];
        long[] fenwick = new long[capacity + 1]; // 1-based
        int[] best = new int[2 * capacity];       // segment tree of positions, leaves at capacity + i

        Watch(String pattern) {
            this.pattern = pattern;
            this.pi = prefixFunction(pattern);
            for (int i = 0; i < lines.size(); i++) count[i] = occurrences(lines.get(i));
            rebuild();
        }

        long occurrences(String text) {
            long found = 0;
            int m = pattern.length();
            for (int i = 0, k = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                while (k > 0 && c != pattern.charAt(k)) k = pi[k - 1];
                if (c == pattern.charAt(k)) k++;
                if (k == m) {
                    found++;
                    k = pi[k - 1];
                }
            }
            return found;
        }

        void grow() {
            count = java.util.Arrays.copyOf(count, capacity);
            fenwick = new long[capacity + 1];
            best = new int[2 * capacity];
            rebuild();
        }

        /** O(capacity) build of both trees from the counts. */
        void rebuild() {
            java.util.Arrays.fill(fenwick, 0);
            for (int i = 1; i <= capacity; i++) {
                fenwick[i] += count[i - 1];
                int parent = i + (i & -i);
                if (parent <= capacity) fenwick[parent] += fenwick[i];
            }
            for (int i = 0; i < capacity; i++) best[capacity + i] = i;
            for (int i = capacity - 1; i >= 1; i--) best[i] = better(best[2 * i], best[2 * i + 1]);
        }

        int better(int a, int b) {
            if (count[a] != count[b]) return count[a] > count[b] ? a : b;
            return Math.min(a, b);
        }

        void set(int line, long value) {
            long delta = value - count[line];
            count[line] = value;
            for (int i = line + 1; i <= capacity; i += i & -i) fenwick[i] += delta;
            for (int i = (line + capacity) / 2; i >= 1; i /= 2) best[i] = better(best[2 * i], best[2 * i + 1]);
        }

        long prefix(int lineCount) {
            long sum = 0;
            for (int i = lineCount; i > 0; i -= i & -i) sum += fenwick[i];
            return sum;
        }

        /** The smallest line whose running total reaches target, or -1. */
        int reaching(long target) {
            int position = 0;
            long remaining = target;
            for (int step = Integer.highestOneBit(capacity); step > 0; step >>= 1) {
                int next = position + step;
                if (next <= capacity && fenwick[next] < remaining) {
                    position = next;
                    remaining -= fenwick[next];
                }
            }
            return position < lines.size() ? position : -1;
        }

        int argMax(int lo, int hi) {
            int result = lo;
            for (int l = lo + capacity, h = hi + capacity; l < h; l /= 2, h /= 2) {
                if ((l & 1) == 1) result = better(result, best[l++]);
                if ((h & 1) == 1) result = better(result, best[--h]);
            }
            return result;
        }
    }

    /** Starts counting a pattern in every line, existing ones included; watching it again does nothing. */
    public void watch(String pattern) {
        if (pattern == null || pattern.isEmpty()) throw new IllegalArgumentException("empty pattern");
        watches.computeIfAbsent(pattern, Watch::new);
    }

    /** Adds a line at the end and returns its number, counting from 0. */
    public int append(String line) {
        Objects.requireNonNull(line, "line");
        checkLine(line);
        int number = lines.size();
        if (number == capacity) {
            capacity *= 2;
            for (Watch w : watches.values()) w.grow();
        }
        lines.add(line);
        for (Watch w : watches.values()) w.set(number, w.occurrences(line));
        return number;
    }

    /** Replaces the text of an existing line. */
    public void replace(int number, String line) {
        Objects.checkIndex(number, lines.size());
        Objects.requireNonNull(line, "line");
        checkLine(line);
        lines.set(number, line);
        for (Watch w : watches.values()) w.set(number, w.occurrences(line));
    }

    public int lines() {
        return lines.size();
    }

    public String line(int number) {
        return lines.get(Objects.checkIndex(number, lines.size()));
    }

    /** Occurrences of a watched pattern, overlapping ones included, in the lines [from, to). */
    public long count(String pattern, int from, int to) {
        Watch w = watched(pattern);
        checkRange(from, to);
        return w.prefix(to) - w.prefix(from);
    }

    /** The first line at or after {@code from} that contains the pattern, or -1. */
    public int firstLineWith(String pattern, int from) {
        Watch w = watched(pattern);
        checkRange(from, lines.size());
        return w.reaching(w.prefix(from) + 1);
    }

    /** The line of [from, to) with the most occurrences, the first one on ties. */
    public int busiestLine(String pattern, int from, int to) {
        Watch w = watched(pattern);
        checkRange(from, to);
        if (from == to) throw new IllegalArgumentException("empty range");
        return w.argMax(from, to);
    }

    private Watch watched(String pattern) {
        Watch w = pattern == null ? null : watches.get(pattern);
        if (w == null) throw new IllegalArgumentException("not watched: " + pattern);
        return w;
    }

    private void checkRange(int from, int to) {
        if (from < 0 || to > lines.size() || from > to) {
            throw new IndexOutOfBoundsException("bad range [" + from + ", " + to + ") of " + lines.size() + " lines");
        }
    }

    private static void checkLine(String line) {
        if (line.indexOf('\n') >= 0) throw new IllegalArgumentException("a line cannot contain a line break");
    }

    private static int[] prefixFunction(String p) {
        int[] pi = new int[p.length()];
        for (int i = 1, k = 0; i < p.length(); i++) {
            while (k > 0 && p.charAt(i) != p.charAt(k)) k = pi[k - 1];
            if (p.charAt(i) == p.charAt(k)) k++;
            pi[i] = k;
        }
        return pi;
    }
}
