package dojo;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A fixed number of page frames between the tree and the disk: a write-back cache of pages
 * with LRU replacement. A fetched page is pinned until it is unpinned, and a pinned page is
 * never evicted.
 */
public final class BufferPool {
    private final PageFile file;
    private final int capacity;
    /** Frames in insertion order; a hit moves its frame to the end, so the first is the LRU. */
    private final Map<Integer, Frame> frames = new LinkedHashMap<>();
    private int nextPageId;
    private long hits;
    private long misses;

    private static final class Frame {
        final Page page;
        int pins = 1;
        boolean dirty;

        Frame(Page page) {
            this.page = page;
        }
    }

    public BufferPool(PageFile file, int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("a buffer pool needs at least one frame");
        }
        this.file = file;
        this.capacity = capacity;
        this.nextPageId = file.pageCount();
    }

    /** The page with this id, pinned; read from the file on a miss. */
    public Page fetch(int pageId) {
        Frame frame = frames.remove(pageId);
        if (frame != null) {
            frames.put(pageId, frame);
            frame.pins++;
            hits++;
            return frame.page;
        }
        if (pageId < 0 || pageId >= nextPageId) {
            throw new IllegalArgumentException("page " + pageId + " does not exist");
        }
        misses++;
        frame = admit(pageId);
        file.read(pageId, frame.page.data());
        return frame.page;
    }

    /** A new zeroed page at the end of the file, pinned and dirty. */
    public Page allocate() {
        Frame frame = admit(nextPageId);
        nextPageId++;
        frame.dirty = true;
        return frame.page;
    }

    /** Releases one pin; {@code dirty} says the caller changed the page. */
    public void unpin(Page page, boolean dirty) {
        Frame frame = frames.get(page.id());
        if (frame == null || frame.page != page || frame.pins == 0) {
            throw new IllegalStateException("page " + page.id() + " is not pinned");
        }
        frame.pins--;
        frame.dirty |= dirty;
    }

    /** Writes every dirty page back and forces the file to disk. */
    public void flush() {
        for (Frame frame : frames.values()) {
            if (frame.dirty) {
                file.write(frame.page.id(), frame.page.data());
                frame.dirty = false;
            }
        }
        file.sync();
    }

    /** How many frames hold a page with at least one pin. */
    public int pinned() {
        return (int) frames.values().stream().filter(frame -> frame.pins > 0).count();
    }

    public long hits() {
        return hits;
    }

    public long misses() {
        return misses;
    }

    public PageFile file() {
        return file;
    }

    private Frame admit(int pageId) {
        if (frames.size() == capacity) {
            evict();
        }
        Frame frame = new Frame(new Page(pageId, ByteBuffer.allocate(PageFile.PAGE_SIZE)));
        frames.put(pageId, frame);
        return frame;
    }

    private void evict() {
        Iterator<Frame> oldestFirst = frames.values().iterator();
        while (oldestFirst.hasNext()) {
            Frame frame = oldestFirst.next();
            if (frame.pins == 0) {
                if (frame.dirty) {
                    file.write(frame.page.id(), frame.page.data());
                }
                oldestFirst.remove();
                return;
            }
        }
        throw new IllegalStateException("every frame is pinned");
    }
}
