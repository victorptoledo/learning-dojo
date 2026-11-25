package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BPlusTreeTest {
    @TempDir
    Path dir;

    @Test
    void aCacheHitDoesNotReadTheFile() {
        try (PageFile file = PageFile.open(dir.resolve("pool.db"))) {
            BufferPool pool = new BufferPool(file, 2);
            Page page = pool.allocate();
            page.data().putLong(0, 42);
            pool.unpin(page, true);
            Page again = pool.fetch(page.id());
            assertEquals(42, again.data().getLong(0));
            pool.unpin(again, false);
            assertEquals(1, pool.hits());
            assertEquals(0, file.reads());
        }
    }

    @Test
    void putGetAndRange() {
        try (PageFile file = PageFile.open(dir.resolve("tree.db"))) {
            BPlusTree tree = BPlusTree.open(new BufferPool(file, 8));
            tree.put(3, 30);
            tree.put(1, 10);
            tree.put(2, 20);
            assertEquals(OptionalLong.of(20), tree.get(2));
            assertTrue(tree.get(4).isEmpty());
            assertEquals(List.of(new Entry(1, 10), new Entry(2, 20)), tree.range(1, 3));
            assertEquals(3, tree.size());
            tree.close();
        }
    }
}
