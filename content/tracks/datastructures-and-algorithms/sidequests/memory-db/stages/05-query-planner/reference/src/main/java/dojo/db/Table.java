package dojo.db;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * A table of {@code long} columns. The rows live only in the {@link RowStore}; the table keeps
 * which row ids are alive (a bitmap) and its indexes. An ordered index maps (value, row id) in a
 * skip list; a bitmap index keeps one bitmap of row ids per distinct value. A query intersects
 * the rows of every indexed predicate as bitmaps and reads only those rows, to check the others.
 * The planner counts what each index would return (ranks in the skip list, cached cardinalities
 * of the bitmaps) without reading anything.
 */
public final class Table {

    private record Key(long value, int row) {
    }

    private static final Comparator<Key> KEY_ORDER = Comparator.comparingLong(Key::value).thenComparingInt(Key::row);

    private final List<String> columns;
    private final Map<String, Integer> position = new HashMap<>();
    private final RowStore store;
    private final Bitmap live = new Bitmap();
    private final Map<Integer, OrderedIndex<Key, Boolean>> ordered = new HashMap<>();
    private final Map<Integer, Map<Long, Bitmap>> bitmaps = new HashMap<>();

    public Table(List<String> columns, RowStore store) {
        if (columns == null || columns.isEmpty() || store == null || new HashSet<>(columns).size() != columns.size()) {
            throw new IllegalArgumentException("columns must be distinct and not empty: " + columns);
        }
        this.columns = List.copyOf(columns);
        for (int i = 0; i < columns.size(); i++) {
            position.put(columns.get(i), i);
        }
        this.store = store;
    }

    public int insert(long... values) {
        if (values == null || values.length != columns.size()) {
            throw new IllegalArgumentException("a row has " + columns.size() + " values");
        }
        int row = store.append(values.clone());
        live.set(row);
        ordered.forEach((column, index) -> index.put(new Key(values[column], row), Boolean.TRUE));
        bitmaps.forEach((column, byValue) -> byValue.computeIfAbsent(values[column], v -> new Bitmap()).set(row));
        return row;
    }

    public long[] get(int row) {
        if (!live.get(row)) {
            throw new IllegalArgumentException("no row " + row);
        }
        return store.read(row);
    }

    public boolean delete(int row) {
        if (!live.get(row)) {
            return false;
        }
        long[] values = store.read(row);
        ordered.forEach((column, index) -> index.remove(new Key(values[column], row)));
        bitmaps.forEach((column, byValue) -> byValue.get(values[column]).clear(row));
        live.clear(row);
        store.delete(row);
        return true;
    }

    public int size() {
        return live.cardinality();
    }

    public void createIndex(String column) {
        int c = column(column);
        if (ordered.containsKey(c)) {
            throw new IllegalArgumentException("already indexed: " + column);
        }
        var index = new OrderedIndex<Key, Boolean>(KEY_ORDER);
        for (int row : live) {
            index.put(new Key(store.read(row)[c], row), Boolean.TRUE);
        }
        ordered.put(c, index);
    }

    public void createBitmapIndex(String column) {
        int c = column(column);
        if (bitmaps.containsKey(c)) {
            throw new IllegalArgumentException("already has a bitmap index: " + column);
        }
        var byValue = new HashMap<Long, Bitmap>();
        for (int row : live) {
            byValue.computeIfAbsent(store.read(row)[c], v -> new Bitmap()).set(row);
        }
        bitmaps.put(c, byValue);
    }

    public long count(List<Predicate> where) {
        var plan = split(where);
        if (plan.rest.isEmpty()) {
            return plan.candidates.cardinality();
        }
        long count = 0;
        for (int row : plan.candidates) {
            if (matchesAll(plan.rest, store.read(row))) {
                count++;
            }
        }
        return count;
    }

    public List<Integer> query(List<Predicate> where) {
        var plan = split(where);
        var result = new ArrayList<Integer>();
        for (int row : plan.candidates) {
            if (plan.rest.isEmpty() || matchesAll(plan.rest, store.read(row))) {
                result.add(row);
            }
        }
        return result;
    }

    public Plan explain(List<Predicate> where) {
        checked(where);
        String best = null;
        long bestRows = 0;
        for (Predicate p : where) {
            long rows = estimate(p);
            if (rows >= 0 && (best == null || rows < bestRows)) {
                best = p.column();
                bestRows = rows;
            }
        }
        return best == null ? new Plan(null, size()) : new Plan(best, bestRows);
    }

    public List<Integer> top(List<Predicate> where, String orderBy, int limit) {
        int c = column(orderBy);
        if (limit < 0) {
            throw new IllegalArgumentException("negative limit " + limit);
        }
        var plan = split(where);
        var result = new ArrayList<Integer>();
        if (limit == 0) {
            return result;
        }
        OrderedIndex<Key, Boolean> index = ordered.get(c);
        if (index != null && plan.rest.isEmpty()) {
            // walk the column in order and stop at the limit: no row is read
            Iterator<Map.Entry<Key, Boolean>> keys = index.range(null, null);
            while (keys.hasNext() && result.size() < limit) {
                int row = keys.next().getKey().row();
                if (plan.candidates.get(row)) {
                    result.add(row);
                }
            }
            return result;
        }
        // the best `limit` rows in a max-heap of their (value, row): the worst kept is evicted
        var best = new PriorityQueue<Key>(KEY_ORDER.reversed());
        for (int row : plan.candidates) {
            long[] values = store.read(row);
            if (!plan.rest.isEmpty() && !matchesAll(plan.rest, values)) {
                continue;
            }
            best.add(new Key(values[c], row));
            if (best.size() > limit) {
                best.poll();
            }
        }
        var kept = new ArrayList<>(best);
        kept.sort(KEY_ORDER);
        for (Key key : kept) {
            result.add(key.row());
        }
        return result;
    }

    /** How many rows an index would give for {@code p}, or -1 when its column has none. */
    private long estimate(Predicate p) {
        int c = column(p.column());
        Map<Long, Bitmap> byValue = bitmaps.get(c);
        if (byValue != null) {
            long rows = 0;
            for (var entry : byValue.entrySet()) {
                if (accepts(p, entry.getKey())) {
                    rows += entry.getValue().cardinality();
                }
            }
            return rows;
        }
        OrderedIndex<Key, Boolean> index = ordered.get(c);
        if (index == null) {
            return -1;
        }
        long rows = 0;
        for (long[] range : ranges(p)) {
            rows += index.rank(new Key(range[1], Integer.MIN_VALUE)) - index.rank(new Key(range[0], Integer.MIN_VALUE));
        }
        return rows;
    }

    /** The rows every indexed predicate allows, and the predicates left to check on the rows. */
    private record Split(Bitmap candidates, List<Predicate> rest) {
    }

    private Split split(List<Predicate> where) {
        checked(where);
        Bitmap candidates = live.copy();
        var rest = new ArrayList<Predicate>();
        for (Predicate p : where) {
            Bitmap rows = fromIndex(p);
            if (rows == null) {
                rest.add(p);
            } else {
                candidates.and(rows);
            }
        }
        return new Split(candidates, rest);
    }

    /** The rows that match {@code p} according to an index, or null when its column has none. */
    private Bitmap fromIndex(Predicate p) {
        int c = column(p.column());
        Map<Long, Bitmap> byValue = bitmaps.get(c);
        if (byValue != null) {
            var rows = new Bitmap();
            byValue.forEach((value, bitmap) -> {
                if (accepts(p, value)) {
                    rows.or(bitmap);
                }
            });
            return rows;
        }
        OrderedIndex<Key, Boolean> index = ordered.get(c);
        if (index == null) {
            return null;
        }
        var rows = new Bitmap();
        for (long[] range : ranges(p)) {
            var keys = index.range(new Key(range[0], Integer.MIN_VALUE), new Key(range[1], Integer.MIN_VALUE));
            while (keys.hasNext()) {
                rows.set(keys.next().getKey().row());
            }
        }
        return rows;
    }

    /** The predicate as half-open ranges of values. */
    private static List<long[]> ranges(Predicate p) {
        return switch (p) {
            case Predicate.Eq eq -> List.of(new long[] {eq.value(), eq.value() + 1});
            case Predicate.Range r -> r.from() < r.to() ? List.of(new long[] {r.from(), r.to()}) : List.of();
            case Predicate.In in -> in.values().stream().map(v -> new long[] {v, v + 1}).toList();
        };
    }

    private static boolean accepts(Predicate p, long value) {
        return switch (p) {
            case Predicate.Eq eq -> value == eq.value();
            case Predicate.Range r -> r.from() <= value && value < r.to();
            case Predicate.In in -> in.values().contains(value);
        };
    }

    private boolean matchesAll(List<Predicate> predicates, long[] values) {
        for (Predicate p : predicates) {
            if (!accepts(p, values[position.get(p.column())])) {
                return false;
            }
        }
        return true;
    }

    private void checked(List<Predicate> where) {
        if (where == null) {
            throw new IllegalArgumentException("no predicates");
        }
        for (Predicate p : where) {
            if (p == null) {
                throw new IllegalArgumentException("a null predicate");
            }
            column(p.column());
        }
    }

    private int column(String name) {
        Integer c = position.get(name);
        if (c == null) {
            throw new IllegalArgumentException("unknown column: " + name);
        }
        return c;
    }
}
