package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ChainedHashMapTest {
    @Test
    void put_get_and_replace() {
        var map = new ChainedHashMap<String, Integer>();
        assertNull(map.put("ada", 36));
        assertEquals(36, map.put("ada", 37));
        assertEquals(37, map.get("ada"));
        assertEquals(1, map.size());
    }

    @Test
    void a_new_map_has_sixteen_buckets() {
        assertEquals(16, new ChainedHashMap<String, String>().bucketCount());
    }
}
