package dojo;

/** Kafka, RabbitMQ, SQS... Publishing can fail. */
public interface MessageBroker {
    void publish(String eventId, String type, String payload);
}
