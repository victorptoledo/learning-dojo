package dojo;

import java.util.Objects;

/**
 * A byte compressor with canonical Huffman codes: greedy code lengths from a heap, canonical
 * codes from the lengths alone, and bits packed most significant first.
 */
public final class Huffman {
    private Huffman() {}

    /** Code lengths per byte value, the number of bits written and the packed bits. Given: no changes needed. */
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

    /** Huffman code lengths for 256 frequencies: 0 for unused symbols, 1 when only one is used. */
    public static int[] codeLengths(long[] frequencies) {
        throw new UnsupportedOperationException("todo codeLengths");
    }

    /** Canonical codes, right-aligned, from the lengths alone; 0 for unused symbols. */
    public static long[] canonicalCodes(int[] lengths) {
        throw new UnsupportedOperationException("todo canonicalCodes");
    }

    /** Compresses bytes into an archive. */
    public static Archive encode(byte[] data) {
        throw new UnsupportedOperationException("todo encode");
    }

    /** Restores the bytes of an archive; a damaged archive throws IllegalArgumentException. */
    public static byte[] decode(Archive archive) {
        throw new UnsupportedOperationException("todo decode");
    }
}
