package dojo;

import java.util.BitSet;

/** A Bloom filter: no false negatives, a tunable rate of false positives. */
public final class BloomFilter {
    private static final double LN2 = Math.log(2);

    private final BitSet bits;
    private final int size;
    private final int hashes;
    private long added;

    public BloomFilter(int bits, int hashes) {
        if (bits < 1 || hashes < 1) {
            throw new IllegalArgumentException("a filter needs at least one bit and one hash");
        }
        this.bits = new BitSet(bits);
        this.size = bits;
        this.hashes = hashes;
    }

    /** A filter sized for {@code expectedItems} at {@code falsePositiveRate}. */
    public static BloomFilter forCapacity(long expectedItems, double falsePositiveRate) {
        if (expectedItems < 1) {
            throw new IllegalArgumentException("expect at least one item");
        }
        if (!(falsePositiveRate > 0 && falsePositiveRate < 1)) {
            throw new IllegalArgumentException("the false-positive rate must be between 0 and 1");
        }
        double m = Math.ceil(-expectedItems * Math.log(falsePositiveRate) / (LN2 * LN2));
        if (m > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("too many bits for one filter: " + m);
        }
        int bits = (int) m;
        int hashes = (int) Math.max(1, Math.round(bits / (double) expectedItems * LN2));
        return new BloomFilter(bits, hashes);
    }

    public void add(String item) {
        long[] h = Hashing.hash128(item);
        for (int i = 0; i < hashes; i++) {
            bits.set(position(h, i));
        }
        added++;
    }

    /** false: never added. true: probably added. */
    public boolean mightContain(String item) {
        long[] h = Hashing.hash128(item);
        for (int i = 0; i < hashes; i++) {
            if (!bits.get(position(h, i))) {
                return false;
            }
        }
        return true;
    }

    public int bitCount() {
        return size;
    }

    public int hashCount() {
        return hashes;
    }

    public int bitsSet() {
        return bits.cardinality();
    }

    public long itemsAdded() {
        return added;
    }

    /** (1 - e^(-k n / m))^k for the items added so far. */
    public double expectedFalsePositiveRate() {
        if (added == 0) {
            return 0;
        }
        return Math.pow(1 - Math.exp(-hashes * (double) added / size), hashes);
    }

    private int position(long[] h, int i) {
        return (int) Math.floorMod(h[0] + i * h[1], (long) size);
    }
}
