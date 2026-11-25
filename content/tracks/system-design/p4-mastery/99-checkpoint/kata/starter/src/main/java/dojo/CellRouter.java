package dojo;

import java.util.List;
import java.util.Map;

/**
 * The data plane of a cell-based service: tenants go to a shuffle shard of cells, health checks
 * eject cells (but never most of them), and the cell map from the control plane is only ever
 * replaced by a newer, valid one.
 */
public final class CellRouter {

    public CellRouter(CellMap initial, ControlPlane controlPlane, Map<String, Cell> cells) {
        throw new UnsupportedOperationException("todo a router starting from map " + initial.version());
    }

    /** The map in use. */
    public CellMap map() {
        throw new UnsupportedOperationException("todo the map in use");
    }

    /** Asks the control plane for a newer map; true only if one was installed. */
    public boolean refresh() {
        throw new UnsupportedOperationException("todo refresh");
    }

    /** Newer maps that were refused because they were not valid. */
    public int rejectedMaps() {
        throw new UnsupportedOperationException("todo rejected maps");
    }

    /** The tenant's cells, in the order they are tried. */
    public List<String> shardOf(String tenant) {
        throw new UnsupportedOperationException("todo the shard of " + tenant);
    }

    /** Health-checks every cell of the map and decides which ones to eject. */
    public void probe() {
        throw new UnsupportedOperationException("todo probe");
    }

    /** True when the last probe found most cells unhealthy and ejected none. */
    public boolean panicking() {
        throw new UnsupportedOperationException("todo panicking");
    }

    /** The ejected cells, sorted by name. */
    public List<String> ejected() {
        throw new UnsupportedOperationException("todo ejected");
    }

    public Response route(Request request) {
        throw new UnsupportedOperationException("todo route " + request);
    }
}
