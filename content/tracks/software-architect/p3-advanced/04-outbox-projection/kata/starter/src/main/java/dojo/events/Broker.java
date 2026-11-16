package dojo.events;

/** A message broker. Publishing may throw when it is down. Do not change it. */
public interface Broker {
    void publish(Event event);
}
