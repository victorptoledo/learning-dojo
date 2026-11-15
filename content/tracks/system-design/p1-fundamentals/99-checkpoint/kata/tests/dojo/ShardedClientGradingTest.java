package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShardedClientGradingTest {
    private final Map<String, MemoryShard> shards = new LinkedHashMap<>();

    private ShardedClient client(int replicas, int threshold, String... names) {
        for (String name : names) {
            shards.put(name, new MemoryShard());
        }
        return new ShardedClient(Map.copyOf(shards), 100, replicas, threshold);
    }

    private static List<String> preference(String key, int replicas, String... names) {
        var ring = new HashRing(100);
        for (String name : names) {
            ring.add(name);
        }
        return ring.nodesFor(key, replicas);
    }

    @Test
    void writes_go_to_the_replicas_of_the_preference_list() {
        var client = client(2, 3, "a", "b", "c", "d");
        for (int i = 0; i < 50; i++) {
            String key = "k" + i;
            assertEquals(2, client.put(key, "v" + i));
            var expected = preference(key, 2, "a", "b", "c", "d");
            for (var entry : shards.entrySet()) {
                assertEquals(expected.contains(entry.getKey()), entry.getValue().holds(key),
                        key + " on " + entry.getKey());
            }
        }
    }

    @Test
    void a_failing_replica_is_marked_down_after_consecutive_failures() {
        var client = client(2, 2, "a", "b", "c");
        var key = "user:1";
        var owners = preference(key, 2, "a", "b", "c");
        var broken = shards.get(owners.get(0));
        broken.failing = true;
        assertEquals(1, client.put(key, "x"), "the other replica still acknowledges");
        assertTrue(client.healthy().contains(owners.get(0)), "one failure is not enough");
        assertEquals(1, client.put(key, "y"));
        assertFalse(client.healthy().contains(owners.get(0)), "two consecutive failures mark it down");
        int calls = broken.puts;
        client.put(key, "z");
        assertEquals(calls, broken.puts, "a shard that is down is not called");
        assertEquals(Optional.of("z"), client.get(key));
    }

    @Test
    void a_success_resets_the_failure_count() {
        var client = client(1, 2, "a", "b", "c");
        var key = "order:9";
        var shard = shards.get(preference(key, 1, "a", "b", "c").get(0));
        shard.failing = true;
        assertThrows(UnavailableException.class, () -> client.put(key, "1"));
        shard.failing = false;
        assertEquals(1, client.put(key, "2"));
        shard.failing = true;
        assertThrows(UnavailableException.class, () -> client.put(key, "3"));
        assertEquals(Set.of("a", "b", "c"), client.healthy(), "fail, succeed, fail is not two in a row");
    }

    @Test
    void reads_fall_back_to_the_next_replica() {
        var client = client(2, 1, "a", "b", "c");
        var key = "cart:5";
        client.put(key, "3 items");
        var owners = preference(key, 2, "a", "b", "c");
        shards.get(owners.get(0)).failing = true;
        assertEquals(Optional.of("3 items"), client.get(key));
        assertFalse(client.healthy().contains(owners.get(0)));
    }

    @Test
    void the_bloom_filters_skip_shards_that_never_saw_the_key() {
        var client = client(2, 3, "a", "b", "c");
        assertEquals(Optional.empty(), client.get("never-written"));
        assertEquals(2, client.skippedByFilter());
        shards.values().forEach(shard -> assertEquals(0, shard.gets, "no shard is asked for an unknown key"));
        client.put("known", "v");
        assertEquals(Optional.of("v"), client.get("known"));
        assertEquals(2, client.skippedByFilter());
    }

    @Test
    void when_every_replica_is_down_writes_fail_and_reads_are_empty() {
        var client = client(2, 1, "a", "b", "c");
        var key = "session:3";
        client.put(key, "token");
        for (String owner : preference(key, 2, "a", "b", "c")) {
            shards.get(owner).failing = true;
        }
        assertThrows(UnavailableException.class, () -> client.put(key, "new"));
        assertEquals(Optional.empty(), client.get(key));
    }

    @Test
    void probe_brings_back_only_shards_that_answer() {
        var client = client(3, 1, "a", "b", "c");
        shards.values().forEach(shard -> shard.failing = true);
        assertThrows(UnavailableException.class, () -> client.put("k", "v"));
        assertEquals(Set.of(), client.healthy());
        shards.get("a").failing = false;
        shards.get("b").failing = false;
        shards.get("b").answersPing = false;
        client.probe();
        assertEquals(Set.of("a"), client.healthy());
        assertEquals(1, client.put("k", "v"));
        int pings = shards.get("a").pings;
        client.probe();
        assertEquals(pings, shards.get("a").pings, "healthy shards are not pinged");
    }

    @Test
    void invalid_clients_are_refused() {
        assertThrows(IllegalArgumentException.class, () -> new ShardedClient(Map.of(), 10, 1, 1));
        Map<String, Shard> one = Map.of("a", new MemoryShard());
        assertThrows(IllegalArgumentException.class, () -> new ShardedClient(one, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ShardedClient(one, 10, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new ShardedClient(one, 10, 1, 0));
    }
}
