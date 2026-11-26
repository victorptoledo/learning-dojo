package dojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * A byte compressor with canonical Huffman codes: greedy code lengths from a heap, canonical
 * codes from the lengths alone, and bits packed most significant first.
 */
public final class Huffman {
    private Huffman() {}

    /** Code lengths per byte value, the number of bits written and the packed bits. */
    public record Archive(int[] lengths, long bitCount, byte[] bits) {
        public Archive {
            Objects.requireNonNull(lengths, "lengths");
            Objects.requireNonNull(bits, "bits");
            lengths = lengths.clone();
            bits = bits.clone();
        }

        @Override
        public int[] lengths() {
            return lengths.clone();
        }

        @Override
        public byte[] bits() {
            return bits.clone();
        }
    }

    public static final int SYMBOLS = 256;
    public static final int MAX_LENGTH = 62;

    /**
     * Huffman code lengths: repeatedly merge the two lightest trees, a tree's key being its
     * total frequency and then the smallest symbol it holds. 0 for unused symbols, 1 when only
     * one symbol is used.
     */
    public static int[] codeLengths(long[] frequencies) {
        if (frequencies == null || frequencies.length != SYMBOLS) throw new IllegalArgumentException("need 256 frequencies");
        int nodes = 0;
        long[] weight = new long[2 * SYMBOLS];
        int[] smallest = new int[2 * SYMBOLS];
        int[] parent = new int[2 * SYMBOLS];
        Arrays.fill(parent, -1);
        PriorityQueue<Integer> heap = new PriorityQueue<>(
                Comparator.<Integer>comparingLong(n -> weight[n]).thenComparingInt(n -> smallest[n]));
        int[] leaf = new int[SYMBOLS];
        Arrays.fill(leaf, -1);
        for (int s = 0; s < SYMBOLS; s++) {
            if (frequencies[s] < 0) throw new IllegalArgumentException("negative frequency for " + s);
            if (frequencies[s] == 0) continue;
            weight[nodes] = frequencies[s];
            smallest[nodes] = s;
            leaf[s] = nodes;
            heap.add(nodes++);
        }
        int[] lengths = new int[SYMBOLS];
        if (nodes == 1) {
            lengths[smallest[0]] = 1;
            return lengths;
        }
        while (heap.size() > 1) {
            int a = heap.poll();
            int b = heap.poll();
            weight[nodes] = Math.addExact(weight[a], weight[b]);
            smallest[nodes] = Math.min(smallest[a], smallest[b]);
            parent[a] = nodes;
            parent[b] = nodes;
            heap.add(nodes++);
        }
        for (int s = 0; s < SYMBOLS; s++) {
            if (leaf[s] < 0) continue;
            int depth = 0;
            for (int n = leaf[s]; parent[n] >= 0; n = parent[n]) depth++;
            lengths[s] = depth;
        }
        return lengths;
    }

    /**
     * Canonical codes: symbols sorted by (length, symbol) get consecutive codes, shifted left
     * whenever the length grows. Codes are right-aligned; 0 for unused symbols.
     */
    public static long[] canonicalCodes(int[] lengths) {
        if (lengths == null || lengths.length != SYMBOLS) throw new IllegalArgumentException("need 256 lengths");
        long[] codes = new long[SYMBOLS];
        long code = 0;
        int previous = 0;
        for (int s : sortedSymbols(lengths)) {
            int length = lengths[s];
            code <<= (length - previous);
            if (code >= (1L << length)) throw new IllegalArgumentException("lengths do not fit in a prefix code");
            codes[s] = code++;
            previous = length;
        }
        return codes;
    }

    /** Compresses bytes into an archive. */
    public static Archive encode(byte[] data) {
        Objects.requireNonNull(data, "data");
        long[] frequencies = new long[SYMBOLS];
        for (byte b : data) frequencies[b & 0xFF]++;
        int[] lengths = codeLengths(frequencies);
        long[] codes = canonicalCodes(lengths);
        long bitCount = 0;
        for (int s = 0; s < SYMBOLS; s++) bitCount += frequencies[s] * lengths[s];
        if ((bitCount + 7) / 8 > Integer.MAX_VALUE - 8) throw new IllegalArgumentException("too large");
        byte[] out = new byte[(int) ((bitCount + 7) / 8)];
        int position = 0;
        long buffer = 0; // pending bits, right-aligned
        int pending = 0;
        for (byte b : data) {
            int s = b & 0xFF;
            int length = lengths[s];
            long code = codes[s];
            while (length > 0) { // write in pieces so the buffer never overflows
                int take = Math.min(length, 32);
                buffer = (buffer << take) | ((code >>> (length - take)) & ((1L << take) - 1));
                pending += take;
                length -= take;
                while (pending >= 8) {
                    pending -= 8;
                    out[position++] = (byte) (buffer >>> pending);
                }
                buffer &= (1L << pending) - 1;
            }
        }
        if (pending > 0) out[position] = (byte) (buffer << (8 - pending));
        return new Archive(lengths, bitCount, out);
    }

    /** Restores the bytes of an archive; a damaged archive throws IllegalArgumentException. */
    public static byte[] decode(Archive archive) {
        Objects.requireNonNull(archive, "archive");
        int[] lengths = archive.lengths();
        byte[] bits = archive.bits();
        if (lengths.length != SYMBOLS) throw new IllegalArgumentException("need 256 lengths");
        for (int length : lengths) {
            if (length < 0 || length > MAX_LENGTH) throw new IllegalArgumentException("bad length " + length);
        }
        long bitCount = archive.bitCount();
        if (bitCount < 0 || bitCount > 8L * bits.length) throw new IllegalArgumentException("bad bit count " + bitCount);
        canonicalCodes(lengths); // refuses lengths that are not a prefix code
        List<Integer> order = sortedSymbols(lengths);
        int maxLength = order.isEmpty() ? 0 : lengths[order.get(order.size() - 1)];
        long[] first = new long[maxLength + 2]; // first canonical code of each length
        int[] count = new int[maxLength + 2];
        int[] offset = new int[maxLength + 2]; // index in order of the first symbol of each length
        for (int s : order) count[lengths[s]]++;
        long code = 0;
        int index = 0;
        for (int length = 1; length <= maxLength; length++) {
            code <<= 1;
            first[length] = code;
            offset[length] = index;
            code += count[length];
            index += count[length];
        }
        int[] symbols = new int[order.size()];
        for (int i = 0; i < symbols.length; i++) symbols[i] = order.get(i);

        ByteSink sink = new ByteSink(bitCount);
        long current = 0;
        int length = 0;
        for (long bit = 0; bit < bitCount; bit++) {
            int value = (bits[(int) (bit >>> 3)] >>> (7 - (int) (bit & 7))) & 1;
            current = (current << 1) | value;
            length++;
            if (length > maxLength) throw new IllegalArgumentException("no code matches at bit " + bit);
            long delta = current - first[length];
            if (delta >= 0 && delta < count[length]) {
                sink.add((byte) symbols[offset[length] + (int) delta]);
                current = 0;
                length = 0;
            }
        }
        if (length != 0) throw new IllegalArgumentException("the bits end in the middle of a code");
        return sink.toArray();
    }

    private static List<Integer> sortedSymbols(int[] lengths) {
        List<Integer> used = new ArrayList<>();
        for (int s = 0; s < SYMBOLS; s++) {
            if (lengths[s] < 0 || lengths[s] > MAX_LENGTH) throw new IllegalArgumentException("bad length " + lengths[s]);
            if (lengths[s] > 0) used.add(s);
        }
        used.sort(Comparator.<Integer>comparingInt(s -> lengths[s]).thenComparingInt(s -> s));
        return used;
    }

    /** A growable byte array. */
    private static final class ByteSink {
        private byte[] data;
        private int size;

        ByteSink(long bitCount) {
            data = new byte[(int) Math.min(1 << 16, Math.max(16, bitCount / 4))];
        }

        void add(byte b) {
            if (size == data.length) data = Arrays.copyOf(data, data.length * 2);
            data[size++] = b;
        }

        byte[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }
}
