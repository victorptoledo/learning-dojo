package dojo;

/**
 * Where a checkout saga is. RESERVING, CHARGING and SHIPPING mean "this step is next (or was running
 * when the process died)". COMPENSATING means a step failed and the earlier ones are being undone.
 */
public enum SagaStep {
    RESERVING, CHARGING, SHIPPING, DONE, COMPENSATING, COMPENSATED;

    public boolean finished() {
        return this == DONE || this == COMPENSATED;
    }
}
