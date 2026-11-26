package dojo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class HuffmanGradingTest {

    /** Huffman by sorting a list of trees before every merge, keyed by (weight, smallest symbol). */
    private static int[] slowLengths(long[] f) {
        record Tree(long weight, int smallest, List<Integer> symbols) {}
        List<Tree> trees = new ArrayList<>();
        for (int s = 0; s < 256; s++) if (f[s] > 0) trees.add(new Tree(f[s], s, List.of(s)));
        int[] lengths = new int[256];
        if (trees.size() == 1) {
            lengths[trees.get(0).smallest()] = 1;
            return lengths;
        }
        while (trees.size() > 1) {
            trees.sort(Comparator.comparingLong(Tree::weight).thenComparingInt(Tree::smallest));
            Tree a = trees.remove(0);
            Tree b = trees.remove(0);
            List<Integer> both = new ArrayList<>(a.symbols());
            both.addAll(b.symbols());
            for (int s : both) lengths[s]++;
            trees.add(new Tree(a.weight() + b.weight(), Math.min(a.smallest(), b.smallest()), both));
        }
        return lengths;
    }

    private static long[] randomFrequencies(Random random) {
        long[] f = new long[256];
        int used = 1 + random.nextInt(random.nextBoolean() ? 6 : 256);
        for (int i = 0; i < used; i++) f[random.nextInt(256)] = 1 + random.nextInt(random.nextBoolean() ? 4 : 1_000);
        return f;
    }

    private static String bitsOf(long code, int length) {
        StringBuilder b = new StringBuilder();
        for (int i = length - 1; i >= 0; i--) b.append((code >>> i) & 1);
        return b.toString();
    }

    @Test
    void lengths_match_a_slow_huffman_with_the_same_tie_rule() {
        Random random = new Random(1);
        for (int round = 0; round < 400; round++) {
            long[] f = randomFrequencies(random);
            assertArrayEquals(slowLengths(f), Huffman.codeLengths(f), "lengths for " + Arrays.toString(f));
        }
        long[] equal = new long[256];
        Arrays.fill(equal, 7);
        int[] lengths = Huffman.codeLengths(equal);
        for (int s = 0; s < 256; s++) assertEquals(8, lengths[s], "256 equal frequencies give 8 bits each");
    }

    @Test
    void ties_go_to_the_tree_with_the_smallest_symbol() {
        long[] f = new long[256];
        f[10] = 1;
        f[20] = 1;
        f[30] = 1;
        // the two trees with the smallest symbols merge first: 10 and 20 get 2 bits, 30 gets 1
        int[] lengths = Huffman.codeLengths(f);
        assertEquals(2, lengths[10]);
        assertEquals(2, lengths[20]);
        assertEquals(1, lengths[30]);
        f[40] = 2;
        // {10,20} (2, smallest 10) merges with 30 (1) ... then with 40 (2): check against the slow version
        assertArrayEquals(slowLengths(f), Huffman.codeLengths(f));
    }

    @Test
    void canonical_codes_follow_length_then_symbol() {
        int[] lengths = new int[256];
        int[] l = {3, 3, 3, 3, 3, 2, 4, 4}; // A..H, the example of RFC 1951
        for (int i = 0; i < l.length; i++) lengths['A' + i] = l[i];
        long[] codes = Huffman.canonicalCodes(lengths);
        String[] expected = {"010", "011", "100", "101", "110", "00", "1110", "1111"};
        for (int i = 0; i < l.length; i++) assertEquals(expected[i], bitsOf(codes['A' + i], l[i]), "code of " + (char) ('A' + i));
        assertEquals(0, codes['Z']);
    }

    @Test
    void canonical_codes_are_prefix_free_on_random_lengths() {
        Random random = new Random(2);
        for (int round = 0; round < 200; round++) {
            int[] lengths = Huffman.codeLengths(randomFrequencies(random));
            long[] codes = Huffman.canonicalCodes(lengths);
            List<String> words = new ArrayList<>();
            for (int s = 0; s < 256; s++) if (lengths[s] > 0) words.add(bitsOf(codes[s], lengths[s]));
            for (String a : words) for (String b : words) if (a != b) assertTrue(!b.startsWith(a), a + " is a prefix of " + b);
        }
    }

    @Test
    void lengths_that_are_not_a_prefix_code_are_refused() {
        int[] three = new int[256];
        three[1] = 1;
        three[2] = 1;
        three[3] = 1;
        assertThrows(IllegalArgumentException.class, () -> Huffman.canonicalCodes(three));
        int[] fine = new int[256];
        fine[5] = 3; // an incomplete code is allowed
        assertEquals(0, Huffman.canonicalCodes(fine)[5]);
        int[] tooLong = new int[256];
        tooLong[0] = 63;
        assertThrows(IllegalArgumentException.class, () -> Huffman.canonicalCodes(tooLong));
        int[] negative = new int[256];
        negative[0] = -1;
        assertThrows(IllegalArgumentException.class, () -> Huffman.canonicalCodes(negative));
        assertThrows(IllegalArgumentException.class, () -> Huffman.canonicalCodes(new int[255]));
        assertThrows(IllegalArgumentException.class, () -> Huffman.codeLengths(new long[257]));
        assertThrows(IllegalArgumentException.class, () -> Huffman.codeLengths(null));
        long[] bad = new long[256];
        bad[3] = -2;
        assertThrows(IllegalArgumentException.class, () -> Huffman.codeLengths(bad));
    }

    @Test
    void round_trips_on_many_inputs() {
        Random random = new Random(3);
        for (int round = 0; round < 300; round++) {
            byte[] data = new byte[random.nextInt(300)];
            int alphabet = 1 + random.nextInt(random.nextBoolean() ? 3 : 256);
            for (int i = 0; i < data.length; i++) data[i] = (byte) (random.nextInt(alphabet) * (256 / alphabet) - 128);
            var archive = Huffman.encode(data);
            long[] f = new long[256];
            for (byte b : data) f[b & 0xFF]++;
            int[] lengths = slowLengths(f);
            long optimal = 0;
            for (int s = 0; s < 256; s++) optimal += f[s] * lengths[s];
            assertEquals(optimal, archive.bitCount(), "an optimal code for " + data.length + " bytes");
            assertEquals((optimal + 7) / 8, archive.bits().length);
            assertArrayEquals(data, Huffman.decode(archive));
        }
    }

    @Test
    void edge_inputs() {
        var empty = Huffman.encode(new byte[0]);
        assertEquals(0, empty.bitCount());
        assertEquals(0, empty.bits().length);
        assertArrayEquals(new byte[0], Huffman.decode(empty));
        byte[] same = new byte[1_001];
        Arrays.fill(same, (byte) 0xE9);
        var one = Huffman.encode(same);
        assertEquals(1_001, one.bitCount());
        assertEquals(1, one.lengths()[0xE9]);
        assertArrayEquals(same, Huffman.decode(one));
        byte[] all = new byte[256 * 3];
        for (int i = 0; i < all.length; i++) all[i] = (byte) i;
        assertArrayEquals(all, Huffman.decode(Huffman.encode(all)));
        // the padding bits of the last byte are zero
        var archive = Huffman.encode(new byte[] {1, 2, 2});
        assertEquals(3, archive.bitCount());
        assertArrayEquals(new byte[] {(byte) 0b0110_0000}, archive.bits()); // 1 → 0, 2 → 1
    }

    @Test
    void deep_trees_from_fibonacci_frequencies() {
        long[] f = new long[256];
        long a = 1, b = 1;
        for (int s = 0; s < 45; s++) {
            f[s] = a;
            long next = a + b;
            a = b;
            b = next;
        }
        int[] lengths = Huffman.codeLengths(f);
        assertArrayEquals(slowLengths(f), lengths);
        assertEquals(44, lengths[0]);
        assertEquals(1, lengths[44]);
        Huffman.canonicalCodes(lengths); // 44 bits still fit
    }

    @Test
    void damaged_archives_are_refused() {
        var good = Huffman.encode(new byte[] {5, 5, 5, 9, 9, 7});
        int[] lengths = good.lengths();
        byte[] bits = good.bits();
        assertThrows(IllegalArgumentException.class, () -> Huffman.decode(new Huffman.Archive(lengths, 8L * bits.length + 1, bits)));
        assertThrows(IllegalArgumentException.class, () -> Huffman.decode(new Huffman.Archive(lengths, -1, bits)));
        assertThrows(IllegalArgumentException.class, () -> Huffman.decode(new Huffman.Archive(new int[255], 0, new byte[0])));
        int[] oversubscribed = lengths.clone();
        oversubscribed[100] = 1;
        assertThrows(IllegalArgumentException.class, () -> Huffman.decode(new Huffman.Archive(oversubscribed, good.bitCount(), bits)));
        // one bit short: the last code is cut in the middle (7 has a 2-bit code)
        assertThrows(IllegalArgumentException.class, () -> Huffman.decode(new Huffman.Archive(lengths, good.bitCount() - 1, bits)));
        // a single-symbol code is "0": a 1 bit matches nothing
        int[] single = new int[256];
        single[65] = 1;
        assertThrows(IllegalArgumentException.class, () -> Huffman.decode(new Huffman.Archive(single, 2, new byte[] {(byte) 0b0100_0000})));
        assertArrayEquals(new byte[] {65, 65}, Huffman.decode(new Huffman.Archive(single, 2, new byte[] {0})));
        assertArrayEquals(new byte[] {5, 5, 5, 9, 9, 7}, Huffman.decode(good));
    }

    @Test
    void forty_megabytes_each_way() {
        Random random = new Random(4);
        byte[] data = new byte[40 * 1024 * 1024];
        for (int i = 0; i < data.length; i++) {
            int r = random.nextInt(100);
            data[i] = (byte) (r < 50 ? 'e' : r < 75 ? 't' : r < 90 ? random.nextInt(26) + 'a' : random.nextInt(256));
        }
        Huffman.Archive[] archive = new Huffman.Archive[1];
        assertTimeoutPreemptively(Duration.ofSeconds(4), () -> {
            archive[0] = Huffman.encode(data);
        }, "encode must pack bits into a byte array, not build a string of '0' and '1'");
        assertTrue(archive[0].bitCount() < 8L * data.length / 2, "the skewed data should compress to less than half");
        assertTimeoutPreemptively(Duration.ofSeconds(4), () -> {
            assertArrayEquals(data, Huffman.decode(archive[0]));
        }, "decode must read the bits with shifts and canonical code ranges, not look up a string per bit");
    }
}
