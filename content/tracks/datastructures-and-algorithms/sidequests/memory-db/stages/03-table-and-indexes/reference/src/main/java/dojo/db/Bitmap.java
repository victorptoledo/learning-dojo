package dojo.db;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

/** A set of row ids as bits in {@code long} words; bit {@code i} lives in word {@code i >>> 6}. */
final class Bitmap implements Iterable<Integer> {

    private long[] words = new long[1];
    private int cardinality;

    void set(int i) {
        int w = i >>> 6;
        if (w >= words.length) {
            words = Arrays.copyOf(words, Math.max(w + 1, words.length * 2));
        }
        long bit = 1L << i;
        if ((words[w] & bit) == 0) {
            words[w] |= bit;
            cardinality++;
        }
    }

    void clear(int i) {
        int w = i >>> 6;
        long bit = 1L << i;
        if (w < words.length && (words[w] & bit) != 0) {
            words[w] &= ~bit;
            cardinality--;
        }
    }

    boolean get(int i) {
        int w = i >>> 6;
        return i >= 0 && w < words.length && (words[w] & 1L << i) != 0;
    }

    int cardinality() {
        return cardinality;
    }

    Bitmap copy() {
        var copy = new Bitmap();
        copy.words = words.clone();
        copy.cardinality = cardinality;
        return copy;
    }

    void and(Bitmap other) {
        int count = 0;
        for (int w = 0; w < words.length; w++) {
            words[w] &= w < other.words.length ? other.words[w] : 0;
            count += Long.bitCount(words[w]);
        }
        cardinality = count;
    }

    void or(Bitmap other) {
        if (other.words.length > words.length) {
            words = Arrays.copyOf(words, other.words.length);
        }
        int count = 0;
        for (int w = 0; w < words.length; w++) {
            if (w < other.words.length) {
                words[w] |= other.words[w];
            }
            count += Long.bitCount(words[w]);
        }
        cardinality = count;
    }

    /** The set bits in ascending order: the lowest bit of a word is {@code word & -word}. */
    @Override
    public PrimitiveIterator.OfInt iterator() {
        return new PrimitiveIterator.OfInt() {
            int w = 0;
            long word = words[0];

            @Override
            public boolean hasNext() {
                while (word == 0 && w + 1 < words.length) {
                    word = words[++w];
                }
                return word != 0;
            }

            @Override
            public int nextInt() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                int bit = Long.numberOfTrailingZeros(word);
                word &= word - 1;
                return (w << 6) + bit;
            }
        };
    }
}
