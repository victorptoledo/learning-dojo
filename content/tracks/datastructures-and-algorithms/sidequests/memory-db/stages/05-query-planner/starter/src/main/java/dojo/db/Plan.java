package dojo.db;

/**
 * How a query would run: the column of the index that drives it and how many rows that index
 * gives, or a null column and every row for a full scan.
 */
public record Plan(String drivingColumn, long estimatedRows) {
}
