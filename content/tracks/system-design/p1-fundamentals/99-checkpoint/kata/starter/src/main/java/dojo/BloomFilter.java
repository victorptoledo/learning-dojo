package dojo;

import java.util.BitSet;

/** A Bloom filter (the Bloom Filter kata, solved). */
public final class BloomFilter {
    private static final double LN2 = Math.log(2);

    private final BitSet bits;
    private final int size;
    private final int hashes;

    public BloomFilter(int bits, int hashes) {
        if (bits < 1 || hashes < 1) {
            throw new IllegalArgumentException("a filter needs at least one bit and one hash");
        }
        this.bits = new BitSet(bits);
        this.size = bits;
        this.hashes = hashes;
    }

    public static BloomFilter forCapacity(long expectedItems, double falsePositiveRate) {
        if (expectedItems < 1 || !(falsePositiveRate > 0 && falsePositiveRate < 1)) {
            throw new IllegalArgumentException("expect at least one item, at a rate between 0 and 1");
        }
        int bits = (int) Math.ceil(-expectedItems * Math.log(falsePositiveRate) / (LN2 * LN2));
        int hashes = (int) Math.max(1, Math.round(bits / (double) expectedItems * LN2));
        return new BloomFilter(bits, hashes);
    }

    public void add(String item) {
        long[] h = Hashing.hash128(item);
        for (int i = 0; i < hashes; i++) {
            bits.set(position(h, i));
        }
    }

    public boolean mightContain(String item) {
        long[] h = Hashing.hash128(item);
        for (int i = 0; i < hashes; i++) {
            if (!bits.get(position(h, i))) {
                return false;
            }
        }
        return true;
    }

    private int position(long[] h, int i) {
        return (int) Math.floorMod(h[0] + i * h[1], (long) size);
    }
}
