package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BPlusTreeGradingTest {
    @TempDir
    Path dir;

    private final List<PageFile> open = new ArrayList<>();

    @AfterEach
    void closeFiles() {
        open.forEach(PageFile::close);
    }

    private PageFile file(String name) {
        PageFile file = PageFile.open(dir.resolve(name));
        open.add(file);
        return file;
    }

    private record Opened(PageFile file, BufferPool pool, BPlusTree tree) {}

    private Opened tree(String name, int frames) {
        PageFile file = file(name);
        BufferPool pool = new BufferPool(file, frames);
        return new Opened(file, pool, BPlusTree.open(pool));
    }

    private static List<Entry> entries(Map<Long, Long> map) {
        return map.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList();
    }

    private static ByteBuffer page(PageFile file, int id) {
        ByteBuffer data = ByteBuffer.allocate(PageFile.PAGE_SIZE);
        file.read(id, data);
        return data;
    }

    private static void assertNothingPinned(BufferPool pool) {
        assertEquals(0, pool.pinned(), "every fetched page must be unpinned before an operation returns");
    }

    // ---- behaviour ----------------------------------------------------------------------

    @Test
    void anEmptyTree() {
        Opened t = tree("empty.db", 8);
        assertTrue(t.tree().get(5).isEmpty());
        assertEquals(0, t.tree().size());
        assertEquals(1, t.tree().height());
        assertEquals(List.of(), t.tree().range(Long.MIN_VALUE, Long.MAX_VALUE));
        assertFalse(t.tree().remove(5));
        assertNothingPinned(t.pool());
    }

    @Test
    void putReplacesAValueWithoutGrowing() {
        BPlusTree tree = tree("replace.db", 8).tree();
        tree.put(7, 1);
        tree.put(7, 2);
        assertEquals(OptionalLong.of(2), tree.get(7));
        assertEquals(1, tree.size());
    }

    @Test
    void extremeAndNegativeKeys() {
        BPlusTree tree = tree("extreme.db", 8).tree();
        long[] keys = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
        for (long key : keys) {
            tree.put(key, ~key);
        }
        for (long key : keys) {
            assertEquals(OptionalLong.of(~key), tree.get(key));
        }
        assertEquals(List.of(new Entry(-1, 0), new Entry(0, -1)), tree.range(-1, 1));
        assertEquals(4, tree.range(Long.MIN_VALUE, Long.MAX_VALUE).size(), "to is exclusive");
    }

    @Test
    void anEmptyOrBackwardRangeIsEmpty() {
        BPlusTree tree = tree("backward.db", 8).tree();
        for (long key = 0; key < 1000; key++) {
            tree.put(key, key);
        }
        assertEquals(List.of(), tree.range(500, 500));
        assertEquals(List.of(), tree.range(600, 400));
    }

    @Test
    void randomOperationsMatchATreeMap() {
        Opened t = tree("random.db", 16);
        BPlusTree tree = t.tree();
        TreeMap<Long, Long> expected = new TreeMap<>();
        Random random = new Random(46);
        for (int i = 0; i < 60_000; i++) {
            long key = random.nextInt(40_000) - 20_000;
            if (random.nextInt(5) == 0) {
                assertEquals(expected.remove(key) != null, tree.remove(key), "remove(" + key + ")");
            } else {
                long value = random.nextLong();
                tree.put(key, value);
                expected.put(key, value);
            }
        }
        assertNothingPinned(t.pool());
        assertEquals(expected.size(), tree.size());
        for (long key = -20_100; key < 20_100; key += 7) {
            Long value = expected.get(key);
            assertEquals(value == null ? OptionalLong.empty() : OptionalLong.of(value), tree.get(key), "get(" + key + ")");
        }
        for (int i = 0; i < 40; i++) {
            long from = random.nextInt(44_000) - 22_000;
            long to = from + random.nextInt(3_000);
            assertEquals(entries(expected.subMap(from, to)), tree.range(from, to), "range(" + from + ", " + to + ")");
        }
        assertEquals(entries(expected), tree.range(Long.MIN_VALUE, Long.MAX_VALUE));
        assertNothingPinned(t.pool());
    }

    @Test
    void rangesSkipLeavesThatRemovalsEmptied() {
        BPlusTree tree = tree("holes.db", 8).tree();
        for (long key = 0; key < 3_000; key++) {
            tree.put(key, key * 10);
        }
        for (long key = 500; key < 2_500; key++) {
            assertTrue(tree.remove(key));
        }
        List<Entry> found = tree.range(400, 2_600);
        assertEquals(200, found.size());
        assertEquals(new Entry(499, 4_990), found.get(99));
        assertEquals(new Entry(2_500, 25_000), found.get(100));
        assertEquals(1_000, tree.size());
        assertTrue(tree.get(1_000).isEmpty());
    }

    @Test
    void theTreeSurvivesClosingAndReopening() {
        Opened first = tree("persist.db", 16);
        TreeMap<Long, Long> expected = new TreeMap<>();
        Random random = new Random(7);
        for (int i = 0; i < 30_000; i++) {
            long key = random.nextLong(1_000_000);
            first.tree().put(key, i);
            expected.put(key, (long) i);
        }
        int height = first.tree().height();
        first.tree().close();
        first.file().close();
        open.remove(first.file());

        Opened second = tree("persist.db", 16);
        assertEquals(expected.size(), second.tree().size());
        assertEquals(height, second.tree().height());
        assertEquals(entries(expected), second.tree().range(0, 1_000_000));
    }

    @Test
    void aFileThatIsNotATreeIsRefused() {
        PageFile file = file("junk.db");
        file.write(0, ByteBuffer.allocate(PageFile.PAGE_SIZE).putInt(0, 0x0BADF00D));
        assertThrows(IllegalStateException.class, () -> BPlusTree.open(new BufferPool(file, 4)));
    }

    // ---- shape and page format ----------------------------------------------------------

    @Test
    void aNewTreeIsAMetaPageAndAnEmptyRootLeaf() {
        Opened t = tree("format.db", 8);
        t.tree().close();
        assertEquals(2, t.file().pageCount());
        ByteBuffer meta = page(t.file(), 0);
        assertEquals(BPlusTree.MAGIC, meta.getInt(0));
        assertEquals(1, meta.getInt(4), "root page");
        assertEquals(1, meta.getInt(8), "height");
        assertEquals(0, meta.getLong(12), "size");
        ByteBuffer root = page(t.file(), 1);
        assertEquals(1, root.get(0), "a leaf");
        assertEquals(0, root.getShort(1));
        assertEquals(0, root.getInt(3), "no next leaf");
    }

    @Test
    void aLeafKeepsItsEntriesSorted() {
        Opened t = tree("leaf.db", 8);
        t.tree().put(5, 50);
        t.tree().put(-1, 10);
        t.tree().put(3, 30);
        t.tree().close();
        ByteBuffer leaf = page(t.file(), 1);
        assertEquals(3, leaf.getShort(1));
        long[] found = new long[6];
        for (int i = 0; i < 3; i++) {
            found[2 * i] = leaf.getLong(7 + 16 * i);
            found[2 * i + 1] = leaf.getLong(7 + 16 * i + 8);
        }
        assertEquals(List.of(-1L, 10L, 3L, 30L, 5L, 50L), java.util.Arrays.stream(found).boxed().toList());
        assertEquals(3, page(t.file(), 0).getLong(12), "size in the meta page");
    }

    @Test
    void aFullLeafSplitsInHalfUnderANewRoot() {
        Opened t = tree("split.db", 8);
        for (long key = 0; key <= BPlusTree.LEAF_CAPACITY; key++) {
            t.tree().put(key, key);
        }
        assertEquals(2, t.tree().height(), "one split, one new root");
        t.tree().close();
        ByteBuffer meta = page(t.file(), 0);
        int rootId = meta.getInt(4);
        assertEquals(2, meta.getInt(8));
        ByteBuffer root = page(t.file(), rootId);
        assertEquals(2, root.get(0), "the root is an internal node");
        assertEquals(1, root.getShort(1), "with one separator");
        assertEquals(128, root.getLong(7), "the separator is the first key of the right leaf");
        ByteBuffer left = page(t.file(), root.getInt(3));
        int rightId = root.getInt(7 + 8);
        ByteBuffer right = page(t.file(), rightId);
        assertEquals(128, left.getShort(1));
        assertEquals(128, right.getShort(1));
        assertEquals(rightId, left.getInt(3), "the left leaf links to the right one");
        assertEquals(0, right.getInt(3));
        assertEquals(128, right.getLong(7));
    }

    @Test
    void aHundredThousandKeysAreThreeLevelsDeep() {
        Opened t = tree("deep.db", 64);
        for (long key = 0; key < 100_000; key++) {
            t.tree().put(key, -key);
        }
        assertEquals(3, t.tree().height());
        t.tree().close();
        int pages = t.file().pageCount();
        assertTrue(pages >= 100_000 / BPlusTree.LEAF_CAPACITY, "too few pages: " + pages);
        assertTrue(pages <= 2 * 100_000 / BPlusTree.LEAF_CAPACITY + 10, "leaves must be at least half full: " + pages + " pages");
    }

    @Test
    void separatorsAreUsedTheWayTheFormatSays() {
        // Keys equal to a separator live in the child to its right.
        Opened t = tree("sep.db", 16);
        for (long key = 0; key < 2_000; key += 2) {
            t.tree().put(key, key);
        }
        t.tree().close();
        ByteBuffer meta = page(t.file(), 0);
        ByteBuffer root = page(t.file(), meta.getInt(4));
        long separator = root.getLong(7);
        ByteBuffer right = page(t.file(), root.getInt(7 + 8));
        assertEquals(separator, right.getLong(7));
    }

    // ---- I/O ----------------------------------------------------------------------------

    private Opened reopened(String name, long keys, int frames) {
        Opened build = tree(name, 64);
        for (long key = 0; key < keys; key++) {
            build.tree().put(key, key);
        }
        build.tree().close();
        build.file().close();
        open.remove(build.file());
        return tree(name, frames);
    }

    @Test
    void aColdLookupReadsOnePagePerLevelAndAWarmOneNone() {
        Opened t = reopened("io.db", 100_000, 64);
        long before = t.file().reads();
        assertEquals(OptionalLong.of(54_321), t.tree().get(54_321));
        assertEquals(t.tree().height(), t.file().reads() - before, "one read per level");
        before = t.file().reads();
        assertEquals(OptionalLong.of(54_322), t.tree().get(54_322));
        assertEquals(0, t.file().reads() - before, "the same path is cached");
        assertNothingPinned(t.pool());
    }

    @Test
    void aRangeScanWalksTheLeafChain() {
        Opened t = reopened("scan.db", 100_000, 64);
        long fetches = t.pool().hits() + t.pool().misses();
        List<Entry> found = t.tree().range(10_000, 30_000);
        assertEquals(20_000, found.size());
        long used = t.pool().hits() + t.pool().misses() - fetches;
        long leaves = 20_000 / 128 + 2;
        assertTrue(used <= t.tree().height() + leaves + 3,
                "a scan descends once and then follows the leaves: " + used + " page fetches");
    }

    @Test
    void aSmallPoolIsEnough() {
        Opened t = tree("small.db", 8);
        for (long key = 0; key < 60_000; key++) {
            t.tree().put(key, key * 3);
        }
        assertEquals(3, t.tree().height());
        assertNothingPinned(t.pool());
        for (long key = 0; key < 60_000; key += 61) {
            assertEquals(OptionalLong.of(key * 3), t.tree().get(key));
        }
        assertEquals(60_000, t.tree().range(0, 60_000).size());
    }

    // ---- structure ----------------------------------------------------------------------

    @Test
    void theTreeReachesTheDiskOnlyThroughThePool() {
        String code = GradingSupport.code("dojo.BPlusTree");
        for (String banned : List.of("PageFile", "FileChannel", "RandomAccessFile", "Files.", "java.io")) {
            assertFalse(code.contains(banned), "BPlusTree must not use " + banned + ": go through the BufferPool");
        }
    }

    @Test
    void theTreeKeepsNoNodesInMemory() {
        for (Class<?> type : GradingSupport.instanceFieldTypes(BPlusTree.class)) {
            assertTrue(type == BufferPool.class || type.isPrimitive(),
                    "BPlusTree may only keep the pool and primitives in fields, not " + type.getSimpleName());
        }
    }

    @Test
    void methodsStayShort() {
        for (String type : List.of("dojo.BPlusTree", "dojo.BufferPool")) {
            List<Integer> lengths = GradingSupport.bodyLengths(GradingSupport.code(type));
            int longest = lengths.stream().mapToInt(Integer::intValue).max().orElse(0);
            assertTrue(longest <= 25, type + " has a method of " + longest + " lines; split it (at most 25)");
        }
    }
}
