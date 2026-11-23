package dojo.db;

/**
 * Where a table keeps its rows, like the pages of a database on disk. Reading a row is the
 * expensive part of a query: the contract tests count every {@code read}.
 */
public interface RowStore {

    /** Stores a row and returns its id: 0, 1, 2... in order. */
    int append(long[] row);

    /** A copy of the row with this id. */
    long[] read(int row);

    /** Forgets the row; its id is never used again. */
    void delete(int row);
}
