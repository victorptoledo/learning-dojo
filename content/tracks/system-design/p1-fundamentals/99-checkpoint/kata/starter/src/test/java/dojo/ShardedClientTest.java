package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ShardedClientTest {
    @Test
    void a_value_written_can_be_read_back() {
        var shards = Map.<String, Shard>of("a", new MemoryShard(), "b", new MemoryShard(), "c", new MemoryShard());
        var client = new ShardedClient(shards, 50, 2, 3);
        assertEquals(2, client.put("user:42", "Ada"));
        assertEquals(Optional.of("Ada"), client.get("user:42"));
    }
}
