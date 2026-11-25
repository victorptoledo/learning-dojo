package dojo;

/**
 * A fixed number of page frames between the tree and the disk: a write-back cache of pages
 * with LRU replacement. A fetched page is pinned until it is unpinned, and a pinned page is
 * never evicted.
 */
public final class BufferPool {
    private final PageFile file;

    public BufferPool(PageFile file, int capacity) {
        this.file = file;
        // todo: check the capacity; the first page you allocate is file.pageCount()
    }

    /** The page with this id, pinned; read from the file on a miss. */
    public Page fetch(int pageId) {
        throw new UnsupportedOperationException("todo: write fetch");
    }

    /** A new zeroed page at the end of the file, pinned and dirty. */
    public Page allocate() {
        throw new UnsupportedOperationException("todo: write allocate");
    }

    /** Releases one pin; {@code dirty} says the caller changed the page. */
    public void unpin(Page page, boolean dirty) {
        throw new UnsupportedOperationException("todo: write unpin");
    }

    /** Writes every dirty page back and forces the file to disk. */
    public void flush() {
        throw new UnsupportedOperationException("todo: write flush");
    }

    /** How many frames hold a page with at least one pin. */
    public int pinned() {
        throw new UnsupportedOperationException("todo: write pinned");
    }

    public long hits() {
        throw new UnsupportedOperationException("todo: count hits");
    }

    public long misses() {
        throw new UnsupportedOperationException("todo: count misses");
    }

    public PageFile file() {
        return file;
    }
}
