package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BufferPoolGradingTest {
    @TempDir
    Path dir;

    private PageFile file;

    @BeforeEach
    void openFile() {
        file = PageFile.open(dir.resolve("pages.db"));
    }

    @AfterEach
    void closeFile() {
        file.close();
    }

    /** Allocates {@code count} pages whose first long is their id, flushes, and returns a fresh pool. */
    private BufferPool poolOver(int count, int capacity) {
        BufferPool writer = new BufferPool(file, count + 1);
        for (int i = 0; i < count; i++) {
            Page page = writer.allocate();
            page.data().putLong(0, page.id());
            writer.unpin(page, true);
        }
        writer.flush();
        return new BufferPool(file, capacity);
    }

    private void touch(BufferPool pool, int pageId) {
        Page page = pool.fetch(pageId);
        assertEquals(pageId, page.data().getLong(0), "page " + pageId + " holds the wrong data");
        pool.unpin(page, false);
    }

    @Test
    void aPoolNeedsAFrame() {
        assertThrows(IllegalArgumentException.class, () -> new BufferPool(file, 0));
    }

    @Test
    void allocateNumbersPagesFromTheEndOfTheFileAndZeroesThem() {
        BufferPool first = new BufferPool(file, 4);
        Page zero = first.allocate();
        Page one = first.allocate();
        assertEquals(0, zero.id());
        assertEquals(1, one.id());
        assertEquals(0, one.data().getLong(4088));
        first.unpin(zero, true);
        first.unpin(one, true);
        first.flush();
        assertEquals(2, file.pageCount());
        assertEquals(2, new BufferPool(file, 4).allocate().id());
    }

    @Test
    void aMissReadsTheFileAndAHitDoesNot() {
        BufferPool pool = poolOver(3, 3);
        long before = file.reads();
        touch(pool, 2);
        touch(pool, 2);
        touch(pool, 0);
        assertEquals(2, file.reads() - before);
        assertEquals(1, pool.hits());
        assertEquals(2, pool.misses());
    }

    @Test
    void twoFetchesOfAPageShareItsData() {
        BufferPool pool = poolOver(1, 2);
        Page first = pool.fetch(0);
        Page second = pool.fetch(0);
        first.data().putLong(8, 99);
        assertEquals(99, second.data().getLong(8));
        assertEquals(1, pool.pinned());
        pool.unpin(first, true);
        assertEquals(1, pool.pinned());
        pool.unpin(second, false);
        assertEquals(0, pool.pinned());
    }

    @Test
    void theLeastRecentlyFetchedPageIsEvicted() {
        BufferPool pool = poolOver(4, 2);
        touch(pool, 0);
        touch(pool, 1);
        touch(pool, 0);           // 1 is now the least recently used
        touch(pool, 2);           // evicts 1
        long misses = pool.misses();
        touch(pool, 0);
        assertEquals(misses, pool.misses(), "page 0 was used more recently than page 1");
        touch(pool, 1);
        assertEquals(misses + 1, pool.misses(), "page 1 should have been evicted");
    }

    @Test
    void aPinnedPageIsNeverEvicted() {
        BufferPool pool = poolOver(4, 2);
        Page held = pool.fetch(0);
        touch(pool, 1);
        touch(pool, 2);
        touch(pool, 3);
        long misses = pool.misses();
        Page again = pool.fetch(0);
        assertEquals(misses, pool.misses(), "a pinned page must stay in the pool");
        pool.unpin(again, false);
        pool.unpin(held, false);
    }

    @Test
    void whenEveryFrameIsPinnedThereIsNoRoom() {
        BufferPool pool = poolOver(3, 2);
        pool.fetch(0);
        pool.fetch(1);
        assertThrows(IllegalStateException.class, () -> pool.fetch(2));
        assertThrows(IllegalStateException.class, pool::allocate);
    }

    @Test
    void aDirtyPageIsWrittenBackOnceWhenEvicted() {
        BufferPool pool = poolOver(3, 1);
        Page page = pool.fetch(0);
        page.data().putLong(16, 1234);
        pool.unpin(page, true);
        long writes = file.writes();
        touch(pool, 1);
        assertEquals(writes + 1, file.writes(), "evicting a dirty page writes it");
        touch(pool, 2);
        assertEquals(writes + 1, file.writes(), "evicting a clean page writes nothing");
        Page back = pool.fetch(0);
        assertEquals(1234, back.data().getLong(16), "the change survived the eviction");
        pool.unpin(back, false);
    }

    @Test
    void anAllocatedPageSurvivesEviction() {
        BufferPool pool = new BufferPool(file, 1);
        Page page = pool.allocate();
        page.data().putLong(0, 77);
        pool.unpin(page, false);   // allocate already made it dirty
        Page other = pool.allocate();
        pool.unpin(other, true);
        Page back = pool.fetch(page.id());
        assertEquals(77, back.data().getLong(0));
        pool.unpin(back, false);
    }

    @Test
    void flushWritesOnlyDirtyPages() {
        BufferPool pool = poolOver(3, 3);
        touch(pool, 0);
        Page page = pool.fetch(1);
        page.data().putLong(8, 5);
        pool.unpin(page, true);
        long writes = file.writes();
        pool.flush();
        assertEquals(writes + 1, file.writes());
        pool.flush();
        assertEquals(writes + 1, file.writes(), "a flushed page is clean again");
    }

    @Test
    void unpinMustMatchAFetch() {
        BufferPool pool = poolOver(2, 2);
        Page page = pool.fetch(0);
        pool.unpin(page, false);
        assertThrows(IllegalStateException.class, () -> pool.unpin(page, false));
    }

    @Test
    void aPageThatDoesNotExistCannotBeFetched() {
        BufferPool pool = poolOver(2, 2);
        assertThrows(IllegalArgumentException.class, () -> pool.fetch(2));
        assertThrows(IllegalArgumentException.class, () -> pool.fetch(-1));
    }

    @Test
    void thePoolNeverHoldsMoreFramesThanItsCapacity() {
        BufferPool pool = poolOver(50, 4);
        for (int round = 0; round < 3; round++) {
            for (int id = 0; id < 50; id++) {
                touch(pool, id);
            }
        }
        assertEquals(150, pool.misses(), "with 4 frames, a scan of 50 pages misses on every page");
    }
}
