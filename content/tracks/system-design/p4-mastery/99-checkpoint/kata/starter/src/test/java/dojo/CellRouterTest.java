package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** A first look at the router: four cells, shards of two. */
class CellRouterTest {
    private final Map<String, FakeCell> fakes = new LinkedHashMap<>();
    private final CellRouter router;

    CellRouterTest() {
        for (String name : List.of("c1", "c2", "c3", "c4")) {
            fakes.put(name, new FakeCell(name));
        }
        ControlPlane down = () -> {
            throw new ControlPlaneDownException("control plane is down");
        };
        router = new CellRouter(new CellMap(1, List.of("c1", "c2", "c3", "c4"), 2), down, Map.copyOf(fakes));
    }

    @Test
    void a_tenant_is_served_by_the_first_cell_of_its_shard() {
        List<String> shard = router.shardOf("acme");
        assertEquals(2, shard.size());
        Response response = router.route(new Request("acme", Priority.NORMAL, "hello"));
        assertEquals(new Response(Outcome.SERVED, shard.get(0), shard.get(0) + ":hello"), response);
    }

    @Test
    void a_poison_request_only_takes_down_its_own_shard() {
        List<String> shard = router.shardOf("evil");
        assertEquals(Outcome.UNAVAILABLE, router.route(new Request("evil", Priority.NORMAL, "poison")).outcome());
        for (FakeCell fake : fakes.values()) {
            assertEquals(shard.contains(fake.name), fake.down, fake.name);
        }
    }

    @Test
    void the_router_keeps_its_map_when_the_control_plane_is_down() {
        assertFalse(router.refresh());
        assertEquals(1, router.map().version());
        assertTrue(router.route(new Request("acme", Priority.CRITICAL, "still here")).outcome() == Outcome.SERVED);
    }
}
