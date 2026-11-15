package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PartitionMapTest {
    @Test
    void the_first_node_owns_every_partition() {
        var map = new PartitionMap(4);
        assertEquals(List.of(), map.addNode("n1"));
        assertEquals("n1", map.ownerOf(3));
    }

    @Test
    void a_second_node_takes_half_the_partitions() {
        var map = new PartitionMap(4);
        map.addNode("n1");
        assertEquals(List.of(new Move(3, "n1", "n2"), new Move(2, "n1", "n2")), map.addNode("n2"));
    }
}
