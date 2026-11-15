package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class PartitionMapGradingTest {
    private static void assertBalanced(PartitionMap map, int partitions) {
        var seen = new TreeSet<Integer>();
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (var entry : map.assignment().entrySet()) {
            for (int p : entry.getValue()) {
                assertTrue(seen.add(p), "partition " + p + " has two owners");
                assertEquals(entry.getKey(), map.ownerOf(p));
            }
            min = Math.min(min, entry.getValue().size());
            max = Math.max(max, entry.getValue().size());
        }
        assertEquals(partitions, seen.size(), "every partition needs an owner");
        assertTrue(max - min <= 1, "unbalanced: " + map.assignment());
    }

    @Test
    void the_worked_example_of_the_statement() {
        var map = new PartitionMap(6);
        assertEquals(List.of(), map.addNode("n1"));
        assertEquals(List.of(new Move(5, "n1", "n2"), new Move(4, "n1", "n2"), new Move(3, "n1", "n2")), map.addNode("n2"));
        assertEquals(List.of(new Move(2, "n1", "n3"), new Move(5, "n2", "n3")), map.addNode("n3"));
        assertEquals(Map.of("n1", List.of(0, 1), "n2", List.of(3, 4), "n3", List.of(2, 5)), map.assignment());
        assertEquals(List.of(new Move(3, "n2", "n1"), new Move(4, "n2", "n3")), map.removeNode("n2"));
        assertEquals(List.of("n1", "n3"), List.copyOf(map.assignment().keySet()));
    }

    @Test
    void adding_a_node_moves_partitions_only_to_it_and_as_few_as_needed() {
        var map = new PartitionMap(256);
        for (int n = 1; n <= 9; n++) {
            String node = "node-" + n;
            var moves = map.addNode(node);
            assertEquals(n == 1 ? 0 : 256 / n, moves.size(), "moves when adding " + node);
            for (Move move : moves) {
                assertEquals(node, move.to());
                assertTrue(!move.from().equals(node));
            }
            assertBalanced(map, 256);
        }
    }

    @Test
    void removing_a_node_moves_only_its_partitions() {
        var map = new PartitionMap(64);
        for (int n = 1; n <= 5; n++) {
            map.addNode("n" + n);
        }
        var before = map.assignment();
        var moves = map.removeNode("n3");
        assertEquals(before.get("n3").size(), moves.size());
        for (Move move : moves) {
            assertEquals("n3", move.from());
            assertTrue(before.get("n3").contains(move.partition()));
        }
        for (String node : List.of("n1", "n2", "n4", "n5")) {
            assertTrue(map.assignment().get(node).containsAll(before.get(node)), node + " lost a partition");
        }
        assertBalanced(map, 64);
    }

    @Test
    void keys_keep_their_partition_and_move_only_with_it() {
        var map = new PartitionMap(32);
        map.addNode("a");
        map.addNode("b");
        var partitionBefore = new HashMap<String, Integer>();
        var nodeBefore = new HashMap<String, String>();
        for (int i = 0; i < 2000; i++) {
            String key = "user-" + i;
            partitionBefore.put(key, map.partitionFor(key));
            nodeBefore.put(key, map.nodeFor(key));
            assertEquals((int) Math.floorMod(Hashing.hash64(key), 32L), map.partitionFor(key));
        }
        var moved = new TreeSet<Integer>();
        map.addNode("c").forEach(move -> moved.add(move.partition()));
        for (var entry : nodeBefore.entrySet()) {
            String key = entry.getKey();
            assertEquals(partitionBefore.get(key), map.partitionFor(key));
            boolean shouldMove = moved.contains(partitionBefore.get(key));
            assertEquals(shouldMove, !entry.getValue().equals(map.nodeFor(key)), key);
        }
    }

    @Test
    void any_sequence_of_joins_and_leaves_stays_balanced() {
        var random = new Random(7);
        var map = new PartitionMap(100);
        var nodes = new java.util.ArrayList<String>();
        int next = 0;
        for (int step = 0; step < 200; step++) {
            if (nodes.size() < 2 || (nodes.size() < 12 && random.nextBoolean())) {
                String node = "n" + next++;
                map.addNode(node);
                nodes.add(node);
            } else {
                map.removeNode(nodes.remove(random.nextInt(nodes.size())));
            }
            assertBalanced(map, 100);
        }
    }

    @Test
    void more_nodes_than_partitions_leave_some_nodes_empty() {
        var map = new PartitionMap(2);
        map.addNode("a");
        map.addNode("b");
        assertEquals(List.of(), map.addNode("c"));
        assertEquals(List.of(), map.assignment().get("c"));
        assertBalanced(map, 2);
        assertEquals(List.of(), map.removeNode("c"));
    }

    @Test
    void invalid_operations_are_refused() {
        assertThrows(IllegalArgumentException.class, () -> new PartitionMap(0));
        var map = new PartitionMap(8);
        assertThrows(IllegalStateException.class, () -> map.ownerOf(0));
        map.addNode("a");
        assertThrows(IllegalArgumentException.class, () -> map.addNode("a"));
        assertThrows(IllegalArgumentException.class, () -> map.removeNode("ghost"));
        assertThrows(IllegalArgumentException.class, () -> map.ownerOf(8));
        assertThrows(IllegalArgumentException.class, () -> map.ownerOf(-1));
        assertThrows(IllegalStateException.class, () -> map.removeNode("a"));
    }
}
