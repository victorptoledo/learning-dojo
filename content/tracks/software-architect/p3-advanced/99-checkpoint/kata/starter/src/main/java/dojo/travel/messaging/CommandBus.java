package dojo.travel.messaging;

/** The channel to the suppliers. Given: do not change. */
public interface CommandBus {
    void send(SupplierCommand command);
}
