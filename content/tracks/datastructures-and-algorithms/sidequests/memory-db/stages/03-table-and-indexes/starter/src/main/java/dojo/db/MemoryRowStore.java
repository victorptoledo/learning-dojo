package dojo.db;

import java.util.ArrayList;
import java.util.List;

/** A {@link RowStore} in a list, for your own tests. */
public final class MemoryRowStore implements RowStore {

    private final List<long[]> rows = new ArrayList<>();

    @Override
    public int append(long[] row) {
        rows.add(row.clone());
        return rows.size() - 1;
    }

    @Override
    public long[] read(int row) {
        long[] values = rows.get(row);
        if (values == null) {
            throw new IllegalArgumentException("deleted row " + row);
        }
        return values.clone();
    }

    @Override
    public void delete(int row) {
        rows.set(row, null);
    }
}
