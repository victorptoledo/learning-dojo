package dojo;

/** One cell: a full, independent copy of the service serving part of the tenants. */
public interface Cell {
    /** Serves a request and returns the response body, or throws CellDownException. */
    String handle(Request request);

    /** A health check: true when the cell says it is healthy. May throw CellDownException. */
    boolean ping();
}
