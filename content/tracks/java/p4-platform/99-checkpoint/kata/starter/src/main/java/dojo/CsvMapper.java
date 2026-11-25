package dojo;

import java.util.List;
import java.util.Map;

/** Builds records from CSV rows, reading the column of each component from its @Column. */
public final class CsvMapper<R extends Record> {

    private CsvMapper() {
    }

    /** Checks the record's annotations and types once and keeps what map() needs. */
    public static <R extends Record> CsvMapper<R> of(Class<R> type) {
        throw new UnsupportedOperationException("write CsvMapper.of");
    }

    /** The column names, in component order. */
    public List<String> columns() {
        throw new UnsupportedOperationException("write CsvMapper.columns");
    }

    /** One record from a row of column name to raw text. */
    public R map(Map<String, String> row) {
        throw new UnsupportedOperationException("write CsvMapper.map");
    }
}
