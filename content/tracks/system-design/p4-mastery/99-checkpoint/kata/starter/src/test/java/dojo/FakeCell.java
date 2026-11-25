package dojo;

import java.util.ArrayList;
import java.util.List;

/** A cell for tests: it can be down, fail its health check, or crash on a poison request. */
final class FakeCell implements Cell {
    final String name;
    final List<Request> handled = new ArrayList<>();
    boolean down;
    boolean failsPing;

    FakeCell(String name) {
        this.name = name;
    }

    @Override
    public String handle(Request request) {
        handled.add(request);
        if (down) {
            throw new CellDownException(name);
        }
        if (request.body().contains("poison")) {
            down = true;
            throw new CellDownException(name);
        }
        return name + ":" + request.body();
    }

    @Override
    public boolean ping() {
        if (down) {
            throw new CellDownException(name);
        }
        return !failsPing;
    }
}
