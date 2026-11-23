package dojo.db;

import java.util.ArrayList;
import java.util.List;

/** A row store that counts reads, as a database counts pages. */
final class CountingStore implements RowStore {

    private final List<long[]> rows = new ArrayList<>();
    long reads;

    @Override
    public int append(long[] row) {
        rows.add(row.clone());
        return rows.size() - 1;
    }

    @Override
    public long[] read(int row) {
        reads++;
        long[] values = row >= 0 && row < rows.size() ? rows.get(row) : null;
        if (values == null) {
            throw new IllegalArgumentException("no row " + row + " in the store");
        }
        return values.clone();
    }

    @Override
    public void delete(int row) {
        rows.set(row, null);
    }
}
