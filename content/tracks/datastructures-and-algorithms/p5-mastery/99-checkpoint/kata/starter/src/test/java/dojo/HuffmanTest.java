package dojo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HuffmanTest {
    @Test
    void lengths_and_canonical_codes() {
        long[] frequencies = new long[256];
        frequencies['a'] = 5;
        frequencies['b'] = 2;
        frequencies['c'] = 1;
        frequencies['d'] = 1;
        int[] lengths = Huffman.codeLengths(frequencies);
        assertEquals(1, lengths['a']);
        assertEquals(2, lengths['b']);
        assertEquals(3, lengths['c']);
        assertEquals(3, lengths['d']);
        long[] codes = Huffman.canonicalCodes(lengths);
        assertEquals(0b0, codes['a']);
        assertEquals(0b10, codes['b']);
        assertEquals(0b110, codes['c']);
        assertEquals(0b111, codes['d']);
    }

    @Test
    void bits_are_packed_most_significant_first() {
        var archive = Huffman.encode(new byte[] {0, 0, 1});
        assertEquals(3, archive.bitCount());
        assertArrayEquals(new byte[] {0b0010_0000}, archive.bits());
    }

    @Test
    void a_round_trip() {
        byte[] text = "abracadabra, said the wizard".getBytes(StandardCharsets.UTF_8);
        var archive = Huffman.encode(text);
        assertArrayEquals(text, Huffman.decode(archive));
    }
}
