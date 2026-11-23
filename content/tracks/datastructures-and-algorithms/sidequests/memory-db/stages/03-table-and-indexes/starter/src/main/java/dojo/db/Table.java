package dojo.db;

import java.util.List;

/** A table of long columns whose rows live in a RowStore. See README.md. */
public final class Table {

    public Table(List<String> columns, RowStore store) {
        // todo
    }

    public int insert(long... values) {
        throw new UnsupportedOperationException("todo");
    }

    public long[] get(int row) {
        throw new UnsupportedOperationException("todo");
    }

    public boolean delete(int row) {
        throw new UnsupportedOperationException("todo");
    }

    public int size() {
        throw new UnsupportedOperationException("todo");
    }

    public void createIndex(String column) {
        throw new UnsupportedOperationException("todo");
    }

    public List<Integer> query(List<Predicate> where) {
        throw new UnsupportedOperationException("todo");
    }
}
