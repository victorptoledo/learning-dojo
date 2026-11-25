package dojo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;

/**
 * A B+tree from {@code long} keys to {@code long} values, stored in the pages of a buffer pool.
 * Every node is a page; nothing is kept in memory between calls except through the pool.
 */
public final class BPlusTree {
    public static final int MAGIC = 0x42504C54;
    public static final int LEAF_CAPACITY = 255;
    public static final int INTERNAL_CAPACITY = 340;

    private static final int META_PAGE = 0;
    private static final byte LEAF = 1;
    private static final byte INTERNAL = 2;
    private static final int HEADER = 7;
    private static final int LEAF_ENTRY = 16;
    private static final int INTERNAL_ENTRY = 12;

    private final BufferPool pool;

    private record Meta(int root, int height, long size) {}

    /** A node read into arrays: {@code values} for a leaf, {@code children} for an internal node. */
    private record Node(long[] keys, long[] values, int[] children, int next) {
        boolean isLeaf() {
            return children == null;
        }
    }

    /** What an insert below reports: whether the key was new, and the split it caused, if any. */
    private record Outcome(boolean added, long separator, int right) {
        static final Outcome REPLACED = new Outcome(false, 0, 0);
        static final Outcome ADDED = new Outcome(true, 0, 0);

        boolean split() {
            return right != 0;
        }
    }

    private BPlusTree(BufferPool pool) {
        this.pool = pool;
    }

    /** Opens the tree in the pool's file, creating an empty tree in an empty file. */
    public static BPlusTree open(BufferPool pool) {
        BPlusTree tree = new BPlusTree(pool);
        if (pool.file().pageCount() == 0) {
            tree.create();
        } else if (tree.magic() != MAGIC) {
            throw new IllegalStateException("not a B+tree file");
        }
        return tree;
    }

    public OptionalLong get(long key) {
        Page leaf = findLeaf(key);
        Node node = read(leaf);
        pool.unpin(leaf, false);
        int index = Arrays.binarySearch(node.keys(), key);
        return index >= 0 ? OptionalLong.of(node.values()[index]) : OptionalLong.empty();
    }

    /** Inserts the key, or replaces its value. */
    public void put(long key, long value) {
        Meta meta = meta();
        Outcome outcome = insert(meta.root(), key, value);
        int root = meta.root();
        int height = meta.height();
        if (outcome.split()) {
            Page page = pool.allocate();
            write(page, new Node(new long[] {outcome.separator()}, null,
                    new int[] {meta.root(), outcome.right()}, 0));
            pool.unpin(page, true);
            root = page.id();
            height++;
        }
        writeMeta(new Meta(root, height, meta.size() + (outcome.added() ? 1 : 0)));
    }

    /** Removes the key from its leaf; leaves are not merged, so a leaf may become empty. */
    public boolean remove(long key) {
        Page leaf = findLeaf(key);
        Node node = read(leaf);
        int index = Arrays.binarySearch(node.keys(), key);
        if (index < 0) {
            pool.unpin(leaf, false);
            return false;
        }
        write(leaf, new Node(without(node.keys(), index), without(node.values(), index), null, node.next()));
        pool.unpin(leaf, true);
        Meta meta = meta();
        writeMeta(new Meta(meta.root(), meta.height(), meta.size() - 1));
        return true;
    }

    /** The entries with {@code from <= key < to}, in key order, walking the leaf chain. */
    public List<Entry> range(long from, long to) {
        List<Entry> found = new ArrayList<>();
        if (from >= to) {
            return found;
        }
        Page leaf = findLeaf(from);
        while (true) {
            Node node = read(leaf);
            pool.unpin(leaf, false);
            for (int i = 0; i < node.keys().length; i++) {
                if (node.keys()[i] >= to) {
                    return found;
                }
                if (node.keys()[i] >= from) {
                    found.add(new Entry(node.keys()[i], node.values()[i]));
                }
            }
            if (node.next() == 0) {
                return found;
            }
            leaf = pool.fetch(node.next());
        }
    }

    public long size() {
        return meta().size();
    }

    public int height() {
        return meta().height();
    }

    /** Writes every changed page to the file and forces it to disk. */
    public void close() {
        pool.flush();
    }

    // ---- the tree ----------------------------------------------------------------------

    private void create() {
        Page meta = pool.allocate();
        Page root = pool.allocate();
        write(root, new Node(new long[0], new long[0], null, 0));
        pool.unpin(root, true);
        pool.unpin(meta, true);
        writeMeta(new Meta(root.id(), 1, 0));
    }

    private Page findLeaf(long key) {
        Page page = pool.fetch(meta().root());
        while (page.data().get(0) == INTERNAL) {
            Node node = read(page);
            pool.unpin(page, false);
            page = pool.fetch(node.children()[childIndex(node.keys(), key)]);
        }
        return page;
    }

    private Outcome insert(int pageId, long key, long value) {
        Page page = pool.fetch(pageId);
        Node node = read(page);
        if (node.isLeaf()) {
            Outcome outcome = insertIntoLeaf(page, node, key, value);
            pool.unpin(page, true);
            return outcome;
        }
        int index = childIndex(node.keys(), key);
        Outcome below = insert(node.children()[index], key, value);
        if (!below.split()) {
            pool.unpin(page, false);
            return below;
        }
        Outcome outcome = insertIntoInternal(page, node, index, below);
        pool.unpin(page, true);
        return outcome;
    }

    private Outcome insertIntoLeaf(Page page, Node node, long key, long value) {
        int index = Arrays.binarySearch(node.keys(), key);
        if (index >= 0) {
            long[] values = node.values().clone();
            values[index] = value;
            write(page, new Node(node.keys(), values, null, node.next()));
            return Outcome.REPLACED;
        }
        int at = -index - 1;
        long[] keys = with(node.keys(), at, key);
        long[] values = with(node.values(), at, value);
        if (keys.length <= LEAF_CAPACITY) {
            write(page, new Node(keys, values, null, node.next()));
            return Outcome.ADDED;
        }
        int half = (keys.length + 1) / 2;
        Page right = pool.allocate();
        write(right, new Node(Arrays.copyOfRange(keys, half, keys.length),
                Arrays.copyOfRange(values, half, values.length), null, node.next()));
        write(page, new Node(Arrays.copyOf(keys, half), Arrays.copyOf(values, half), null, right.id()));
        pool.unpin(right, true);
        return new Outcome(true, keys[half], right.id());
    }

    private Outcome insertIntoInternal(Page page, Node node, int index, Outcome below) {
        long[] keys = with(node.keys(), index, below.separator());
        int[] children = with(node.children(), index + 1, below.right());
        if (keys.length <= INTERNAL_CAPACITY) {
            write(page, new Node(keys, null, children, 0));
            return new Outcome(below.added(), 0, 0);
        }
        int half = (keys.length + 1) / 2 - 1;
        Page right = pool.allocate();
        write(right, new Node(Arrays.copyOfRange(keys, half + 1, keys.length), null,
                Arrays.copyOfRange(children, half + 1, children.length), 0));
        write(page, new Node(Arrays.copyOf(keys, half), null, Arrays.copyOf(children, half + 1), 0));
        pool.unpin(right, true);
        return new Outcome(below.added(), keys[half], right.id());
    }

    /** The child to follow: keys equal to a separator live in the child to its right. */
    private static int childIndex(long[] keys, long key) {
        int index = Arrays.binarySearch(keys, key);
        return index >= 0 ? index + 1 : -index - 1;
    }

    // ---- the page format ---------------------------------------------------------------

    private Node read(Page page) {
        ByteBuffer data = page.data();
        int count = data.getShort(1);
        long[] keys = new long[count];
        if (data.get(0) == LEAF) {
            long[] values = new long[count];
            for (int i = 0; i < count; i++) {
                keys[i] = data.getLong(HEADER + i * LEAF_ENTRY);
                values[i] = data.getLong(HEADER + i * LEAF_ENTRY + 8);
            }
            return new Node(keys, values, null, data.getInt(3));
        }
        int[] children = new int[count + 1];
        children[0] = data.getInt(3);
        for (int i = 0; i < count; i++) {
            keys[i] = data.getLong(HEADER + i * INTERNAL_ENTRY);
            children[i + 1] = data.getInt(HEADER + i * INTERNAL_ENTRY + 8);
        }
        return new Node(keys, null, children, 0);
    }

    private void write(Page page, Node node) {
        ByteBuffer data = page.data();
        data.put(0, node.isLeaf() ? LEAF : INTERNAL);
        data.putShort(1, (short) node.keys().length);
        data.putInt(3, node.isLeaf() ? node.next() : node.children()[0]);
        for (int i = 0; i < node.keys().length; i++) {
            if (node.isLeaf()) {
                data.putLong(HEADER + i * LEAF_ENTRY, node.keys()[i]);
                data.putLong(HEADER + i * LEAF_ENTRY + 8, node.values()[i]);
            } else {
                data.putLong(HEADER + i * INTERNAL_ENTRY, node.keys()[i]);
                data.putInt(HEADER + i * INTERNAL_ENTRY + 8, node.children()[i + 1]);
            }
        }
    }

    private int magic() {
        Page page = pool.fetch(META_PAGE);
        int magic = page.data().getInt(0);
        pool.unpin(page, false);
        return magic;
    }

    private Meta meta() {
        Page page = pool.fetch(META_PAGE);
        ByteBuffer data = page.data();
        Meta meta = new Meta(data.getInt(4), data.getInt(8), data.getLong(12));
        pool.unpin(page, false);
        return meta;
    }

    private void writeMeta(Meta meta) {
        Page page = pool.fetch(META_PAGE);
        page.data().putInt(0, MAGIC).putInt(4, meta.root()).putInt(8, meta.height()).putLong(12, meta.size());
        pool.unpin(page, true);
    }

    private static long[] with(long[] items, int at, long item) {
        long[] result = new long[items.length + 1];
        System.arraycopy(items, 0, result, 0, at);
        result[at] = item;
        System.arraycopy(items, at, result, at + 1, items.length - at);
        return result;
    }

    private static int[] with(int[] items, int at, int item) {
        int[] result = new int[items.length + 1];
        System.arraycopy(items, 0, result, 0, at);
        result[at] = item;
        System.arraycopy(items, at, result, at + 1, items.length - at);
        return result;
    }

    private static long[] without(long[] items, int at) {
        long[] result = new long[items.length - 1];
        System.arraycopy(items, 0, result, 0, at);
        System.arraycopy(items, at + 1, result, at, items.length - at - 1);
        return result;
    }
}
