package dojo;

/** One way of reaching people: the strategy the notifier picks by name. */
@FunctionalInterface
public interface Channel {
    /** Delivers the message or throws {@link DeliveryException}. */
    void deliver(Message message);
}
