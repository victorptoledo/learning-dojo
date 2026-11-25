package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CellRouterGradingTest {

    /** A cell that can be down, fail its health check, or crash on a poison request. */
    private static final class GradingCell implements Cell {
        final String name;
        final List<Request> handled = new ArrayList<>();
        boolean down;
        boolean failsPing;
        int pings;

        GradingCell(String name) {
            this.name = name;
        }

        @Override
        public String handle(Request request) {
            handled.add(request);
            if (down) throw new CellDownException(name);
            if (request.body().contains("poison")) {
                down = true;
                throw new CellDownException(name);
            }
            return name + ":" + request.body();
        }

        @Override
        public boolean ping() {
            pings++;
            if (down) throw new CellDownException(name);
            return !failsPing;
        }
    }

    /** A control plane that publishes whatever the test sets, and counts the calls. */
    private static final class GradingControlPlane implements ControlPlane {
        CellMap next;
        boolean down;
        int calls;

        @Override
        public CellMap latest() {
            calls++;
            if (down) throw new ControlPlaneDownException("control plane is down");
            return next;
        }
    }

    private final Map<String, GradingCell> cells = new LinkedHashMap<>();
    private final GradingControlPlane controlPlane = new GradingControlPlane();

    private List<String> names(int count) {
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String name = "c" + i;
            names.add(name);
            cells.computeIfAbsent(name, GradingCell::new);
        }
        return names;
    }

    private CellRouter router(int count, int shardSize) {
        List<String> names = names(count);
        return new CellRouter(new CellMap(1, names, shardSize), controlPlane, Map.copyOf(cells));
    }

    private static Request normal(String tenant) {
        return new Request(tenant, Priority.NORMAL, "work of " + tenant);
    }

    private static List<String> expectedShard(String tenant, List<String> cells, int k) {
        return cells.stream()
                .sorted(Comparator.comparingLong((String cell) -> Hashing.hash64(tenant + "|" + cell))
                        .thenComparing(Comparator.naturalOrder()))
                .limit(k)
                .toList();
    }

    // ---- the map --------------------------------------------------------------------

    @Test
    void the_initial_map_must_be_valid() {
        List<String> names = names(4);
        Map<String, Cell> all = Map.copyOf(cells);
        assertThrows(IllegalArgumentException.class, () -> new CellRouter(new CellMap(0, names, 2), controlPlane, all));
        assertThrows(IllegalArgumentException.class, () -> new CellRouter(new CellMap(1, List.of(), 1), controlPlane, all));
        assertThrows(IllegalArgumentException.class, () -> new CellRouter(new CellMap(1, List.of("c1", "c1"), 1), controlPlane, all));
        assertThrows(IllegalArgumentException.class, () -> new CellRouter(new CellMap(1, List.of("c1", "c9"), 1), controlPlane, all));
        assertThrows(IllegalArgumentException.class, () -> new CellRouter(new CellMap(1, names, 0), controlPlane, all));
        assertThrows(IllegalArgumentException.class, () -> new CellRouter(new CellMap(1, names, 5), controlPlane, all));
        CellRouter router = new CellRouter(new CellMap(3, names, 4), controlPlane, all);
        assertEquals(3, router.map().version());
        assertEquals(0, controlPlane.calls, "a router starts from the map it is given, without the control plane");
    }

    @Test
    void a_shard_is_the_k_cells_with_the_lowest_score_for_the_tenant() {
        CellRouter router = router(8, 3);
        List<String> all = router.map().cells();
        for (int i = 0; i < 200; i++) {
            String tenant = "tenant-" + i;
            assertEquals(expectedShard(tenant, all, 3), router.shardOf(tenant), tenant);
        }
        assertEquals(router.shardOf("acme"), router.shardOf("acme"), "a shard is stable");
    }

    @Test
    void a_new_cell_only_takes_tenants_onto_itself() {
        CellRouter router = router(8, 2);
        List<List<String>> before = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            before.add(router.shardOf("t" + i));
        }
        List<String> nine = names(9);
        CellRouter grown = new CellRouter(new CellMap(1, nine.subList(0, 8), 2), controlPlane, Map.copyOf(cells));
        controlPlane.next = new CellMap(2, nine, 2);
        assertTrue(grown.refresh());
        assertEquals(2, grown.map().version());
        int moved = 0;
        for (int i = 0; i < 500; i++) {
            List<String> after = grown.shardOf("t" + i);
            Set<String> added = new HashSet<>(after);
            added.removeAll(before.get(i));
            assertTrue(added.isEmpty() || added.equals(Set.of("c9")), "t" + i + " may only gain c9");
            if (!added.isEmpty()) moved++;
        }
        assertTrue(moved > 50 && moved < 180, "about 2 in 9 tenants get the new cell, got " + moved);
    }

    @Test
    void only_a_newer_valid_map_replaces_the_one_in_use() {
        CellRouter router = router(4, 2);
        CellMap original = router.map();
        controlPlane.next = original;
        assertFalse(router.refresh(), "the same version is not news");
        controlPlane.next = new CellMap(0, List.of("c1"), 1);
        assertFalse(router.refresh(), "an older map is a replay or a stale replica of the control plane");
        assertEquals(0, router.rejectedMaps(), "old maps are ignored, not counted as rejected");

        controlPlane.next = new CellMap(5, List.of("c1", "c7"), 1);
        assertFalse(router.refresh(), "a cell the router cannot reach");
        controlPlane.next = new CellMap(6, List.of("c1", "c2"), 3);
        assertFalse(router.refresh(), "a shard bigger than the map");
        controlPlane.next = new CellMap(7, List.of(), 1);
        assertFalse(router.refresh(), "no cells at all");
        controlPlane.next = new CellMap(8, List.of("c2", "c2", "c3"), 1);
        assertFalse(router.refresh(), "a repeated cell");
        assertEquals(4, router.rejectedMaps());
        assertEquals(original, router.map(), "a bad push must not take the data plane down");

        controlPlane.next = new CellMap(9, List.of("c1", "c2", "c3"), 2);
        assertTrue(router.refresh());
        assertEquals(9, router.map().version());
    }

    @Test
    void the_data_plane_does_not_need_the_control_plane() {
        CellRouter router = router(6, 2);
        controlPlane.down = true;
        assertFalse(router.refresh());
        int calls = controlPlane.calls;
        for (int i = 0; i < 100; i++) {
            assertEquals(Outcome.SERVED, router.route(normal("t" + i)).outcome());
        }
        router.probe();
        assertEquals(calls, controlPlane.calls, "routing and probing never call the control plane");
        assertEquals(1, router.map().version());
    }

    // ---- routing and failure --------------------------------------------------------

    @Test
    void a_failing_cell_is_skipped_within_the_shard() {
        CellRouter router = router(8, 2);
        List<String> shard = router.shardOf("acme");
        cells.get(shard.get(0)).down = true;
        Response response = router.route(normal("acme"));
        assertEquals(new Response(Outcome.SERVED, shard.get(1), shard.get(1) + ":work of acme"), response);

        cells.get(shard.get(1)).down = true;
        Response none = router.route(normal("acme"));
        assertEquals(Outcome.UNAVAILABLE, none.outcome());
        assertNull(none.cell());
        assertNull(none.body());
        for (GradingCell cell : cells.values()) {
            if (!shard.contains(cell.name)) {
                assertEquals(List.of(), cell.handled, "a request never leaves its tenant's shard: " + cell.name);
            }
        }
    }

    @Test
    void a_poison_tenant_only_hurts_tenants_with_exactly_its_shard() {
        CellRouter router = router(8, 2);
        Set<String> evil = new HashSet<>(router.shardOf("evil"));
        assertEquals(Outcome.UNAVAILABLE, router.route(new Request("evil", Priority.CRITICAL, "poison")).outcome());
        router.probe();
        assertEquals(evil.stream().sorted().toList(), router.ejected());

        int unavailable = 0;
        for (int i = 0; i < 1000; i++) {
            String tenant = "t" + i;
            Set<String> shard = new HashSet<>(router.shardOf(tenant));
            Response response = router.route(normal(tenant));
            if (shard.equals(evil)) {
                assertEquals(Outcome.UNAVAILABLE, response.outcome(), tenant);
                unavailable++;
            } else {
                assertEquals(Outcome.SERVED, response.outcome(), tenant);
                assertFalse(evil.contains(response.cell()), tenant + " must not be sent to an ejected cell");
            }
        }
        assertTrue(unavailable > 10 && unavailable < 80, "about 1 in 28 tenants share the whole shard, got " + unavailable);
        for (GradingCell cell : cells.values()) {
            boolean sawPoison = cell.handled.stream().anyMatch(r -> r.body().contains("poison"));
            assertEquals(evil.contains(cell.name), sawPoison, cell.name);
        }
    }

    @Test
    void ejected_cells_are_not_called_at_all() {
        CellRouter router = router(8, 2);
        List<String> shard = router.shardOf("acme");
        for (String name : shard) {
            cells.get(name).failsPing = true;
        }
        router.probe();
        assertEquals(Outcome.UNAVAILABLE, router.route(new Request("acme", Priority.CRITICAL, "x")).outcome());
        for (String name : shard) {
            assertEquals(List.of(), cells.get(name).handled, name);
        }
        for (String name : shard) {
            cells.get(name).failsPing = false;
        }
        router.probe();
        assertEquals(List.of(), router.ejected(), "a cell that is healthy again comes back");
        assertEquals(Outcome.SERVED, router.route(normal("acme")).outcome());
    }

    // ---- degradation ----------------------------------------------------------------

    @Test
    void a_shard_short_of_cells_sheds_batch_work_only() {
        CellRouter router = router(8, 2);
        List<String> shard = router.shardOf("acme");
        cells.get(shard.get(0)).failsPing = true;
        router.probe();
        assertEquals(List.of(shard.get(0)), router.ejected());

        int callsBefore = cells.get(shard.get(1)).handled.size();
        assertEquals(new Response(Outcome.SHED, null, null), router.route(new Request("acme", Priority.BATCH, "report")));
        assertEquals(callsBefore, cells.get(shard.get(1)).handled.size(), "shed work costs the cells nothing");
        assertEquals(shard.get(1), router.route(new Request("acme", Priority.NORMAL, "a")).cell());
        assertEquals(shard.get(1), router.route(new Request("acme", Priority.CRITICAL, "b")).cell());

        String untouched = null;
        for (int i = 0; untouched == null; i++) {
            if (!router.shardOf("t" + i).contains(shard.get(0))) untouched = "t" + i;
        }
        assertEquals(Outcome.SERVED, router.route(new Request(untouched, Priority.BATCH, "report")).outcome(),
                "a full shard keeps its batch work");
    }

    @Test
    void when_most_cells_look_sick_the_router_trusts_none_of_the_checks() {
        CellRouter router = router(8, 2);
        for (int i = 1; i <= 5; i++) {
            cells.get("c" + i).failsPing = true;  // the checker is wrong: the cells still serve
        }
        router.probe();
        assertTrue(router.panicking());
        assertEquals(List.of(), router.ejected(), "ejecting 5 of 8 cells would overload the other 3");
        assertEquals(Outcome.SERVED, router.route(new Request("acme", Priority.CRITICAL, "pay")).outcome());
        assertEquals(Outcome.SHED, router.route(new Request("acme", Priority.NORMAL, "browse")).outcome());
        assertEquals(Outcome.SHED, router.route(new Request("acme", Priority.BATCH, "report")).outcome());

        cells.get("c5").failsPing = false;
        router.probe();
        assertFalse(router.panicking(), "exactly half is still trusted");
        assertEquals(List.of("c1", "c2", "c3", "c4"), router.ejected());
    }

    @Test
    void a_cell_removed_from_the_map_is_no_longer_ejected_or_probed() {
        CellRouter router = router(4, 1);
        cells.get("c3").failsPing = true;
        router.probe();
        assertEquals(List.of("c3"), router.ejected());
        controlPlane.next = new CellMap(2, List.of("c1", "c2", "c4"), 1);
        assertTrue(router.refresh());
        assertEquals(List.of(), router.ejected());
        int pings = cells.get("c3").pings;
        router.probe();
        assertEquals(pings, cells.get("c3").pings, "only the cells of the map are checked");
        for (int i = 0; i < 50; i++) {
            assertFalse(router.shardOf("t" + i).contains("c3"));
        }
    }

    // ---- structure ------------------------------------------------------------------

    @Test
    void the_router_keeps_no_global_state() {
        for (Field field : CellRouter.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                assertTrue(Modifier.isFinal(field.getModifiers()),
                        "static field " + field.getName() + " would be shared by every router");
            }
        }
    }
}
