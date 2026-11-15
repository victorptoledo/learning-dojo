package dojo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BloomFilterTest {
    @Test
    void an_added_item_is_always_found() {
        var filter = new BloomFilter(1024, 3);
        filter.add("user:42");
        assertTrue(filter.mightContain("user:42"));
    }

    @Test
    void an_empty_filter_contains_nothing() {
        assertFalse(new BloomFilter(1024, 3).mightContain("user:42"));
    }
}
