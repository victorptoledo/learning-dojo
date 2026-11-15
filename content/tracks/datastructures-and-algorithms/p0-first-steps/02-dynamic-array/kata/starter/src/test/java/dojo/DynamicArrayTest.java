package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DynamicArrayTest {
    @Test
    void appends_and_reads_back() {
        var list = new DynamicArray<String>();
        list.add("a");
        list.add("b");
        assertEquals(2, list.size());
        assertEquals("b", list.get(1));
    }

    @Test
    void doubles_when_full() {
        var list = new DynamicArray<Integer>();
        assertEquals(4, list.capacity());
        for (int i = 0; i < 5; i++) {
            list.add(i);
        }
        assertEquals(8, list.capacity());
    }
}
