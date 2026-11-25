package dojo;

import java.util.List;
import java.util.OptionalLong;

/**
 * A B+tree from {@code long} keys to {@code long} values, stored in the pages of a buffer pool.
 * Every node is a page; nothing is kept in memory between calls except through the pool.
 * The page format is in the README.
 */
public final class BPlusTree {
    public static final int MAGIC = 0x42504C54;
    public static final int LEAF_CAPACITY = 255;
    public static final int INTERNAL_CAPACITY = 340;

    private final BufferPool pool;

    private BPlusTree(BufferPool pool) {
        this.pool = pool;
    }

    /** Opens the tree in the pool's file, creating an empty tree in an empty file. */
    public static BPlusTree open(BufferPool pool) {
        throw new UnsupportedOperationException("todo: write open");
    }

    public OptionalLong get(long key) {
        throw new UnsupportedOperationException("todo: write get");
    }

    /** Inserts the key, or replaces its value. */
    public void put(long key, long value) {
        throw new UnsupportedOperationException("todo: write put");
    }

    /** Removes the key from its leaf; leaves are not merged, so a leaf may become empty. */
    public boolean remove(long key) {
        throw new UnsupportedOperationException("todo: write remove");
    }

    /** The entries with {@code from <= key < to}, in key order, walking the leaf chain. */
    public List<Entry> range(long from, long to) {
        throw new UnsupportedOperationException("todo: write range");
    }

    public long size() {
        throw new UnsupportedOperationException("todo: write size");
    }

    public int height() {
        throw new UnsupportedOperationException("todo: write height");
    }

    /** Writes every changed page to the file and forces it to disk. */
    public void close() {
        throw new UnsupportedOperationException("todo: write close");
    }
}
