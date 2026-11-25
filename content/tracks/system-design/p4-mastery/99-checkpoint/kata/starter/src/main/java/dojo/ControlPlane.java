package dojo;

/** The service that publishes cell maps. It is not on the request path. */
public interface ControlPlane {
    /** The newest map it has, or ControlPlaneDownException. */
    CellMap latest();
}
