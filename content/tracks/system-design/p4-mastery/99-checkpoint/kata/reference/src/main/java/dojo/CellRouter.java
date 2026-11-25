package dojo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The data plane of a cell-based service: tenants go to a shuffle shard of cells, health checks
 * eject cells (but never most of them), and the cell map from the control plane is only ever
 * replaced by a newer, valid one.
 */
public final class CellRouter {
    private final ControlPlane controlPlane;
    private final Map<String, Cell> cells;
    private CellMap map;
    private Set<String> ejected = new TreeSet<>();
    private boolean panicking;
    private int rejectedMaps;

    public CellRouter(CellMap initial, ControlPlane controlPlane, Map<String, Cell> cells) {
        this.controlPlane = controlPlane;
        this.cells = Map.copyOf(cells);
        if (!valid(initial)) {
            throw new IllegalArgumentException("the initial cell map is not valid");
        }
        this.map = initial;
    }

    public CellMap map() {
        return map;
    }

    public boolean refresh() {
        CellMap latest;
        try {
            latest = controlPlane.latest();
        } catch (ControlPlaneDownException e) {
            return false;
        }
        if (latest == null || latest.version() <= map.version()) {
            return false;
        }
        if (!valid(latest)) {
            rejectedMaps++;
            return false;
        }
        map = latest;
        ejected.retainAll(latest.cells());
        return true;
    }

    public int rejectedMaps() {
        return rejectedMaps;
    }

    public List<String> shardOf(String tenant) {
        return map.cells().stream()
                .sorted(Comparator.comparingLong((String cell) -> Hashing.hash64(tenant + "|" + cell))
                        .thenComparing(Comparator.naturalOrder()))
                .limit(map.shardSize())
                .toList();
    }

    public void probe() {
        Set<String> unhealthy = new TreeSet<>();
        for (String name : map.cells()) {
            boolean healthy;
            try {
                healthy = cells.get(name).ping();
            } catch (CellDownException e) {
                healthy = false;
            }
            if (!healthy) {
                unhealthy.add(name);
            }
        }
        panicking = unhealthy.size() * 2 > map.cells().size();
        ejected = panicking ? new TreeSet<>() : unhealthy;
    }

    public boolean panicking() {
        return panicking;
    }

    public List<String> ejected() {
        return List.copyOf(ejected);
    }

    public Response route(Request request) {
        List<String> shard = shardOf(request.tenant());
        List<String> available = new ArrayList<>();
        for (String cell : shard) {
            if (!ejected.contains(cell)) {
                available.add(cell);
            }
        }
        if (shed(request.priority(), available.size() < shard.size())) {
            return new Response(Outcome.SHED, null, null);
        }
        for (String name : available) {
            try {
                return new Response(Outcome.SERVED, name, cells.get(name).handle(request));
            } catch (CellDownException e) {
                // try the next cell of the shard, never one outside it
            }
        }
        return new Response(Outcome.UNAVAILABLE, null, null);
    }

    private boolean shed(Priority priority, boolean degraded) {
        if (panicking) {
            return priority != Priority.CRITICAL;
        }
        return degraded && priority == Priority.BATCH;
    }

    private boolean valid(CellMap candidate) {
        if (candidate == null || candidate.version() < 1 || candidate.cells() == null || candidate.cells().isEmpty()) {
            return false;
        }
        if (new HashSet<>(candidate.cells()).size() != candidate.cells().size()) {
            return false;
        }
        if (!cells.keySet().containsAll(candidate.cells())) {
            return false;
        }
        return candidate.shardSize() >= 1 && candidate.shardSize() <= candidate.cells().size();
    }
}
