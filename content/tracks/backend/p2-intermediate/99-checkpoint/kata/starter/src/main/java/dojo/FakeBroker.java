package dojo;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** A broker for the kata: remembers what it published and can be told to fail. */
@Component
public class FakeBroker implements MessageBroker {
    public record Message(String eventId, String type, String payload) {}

    private final List<Message> published = new ArrayList<>();
    private int failuresLeft;

    @Override
    public synchronized void publish(String eventId, String type, String payload) {
        if (failuresLeft > 0) {
            failuresLeft--;
            throw new IllegalStateException("broker unavailable");
        }
        published.add(new Message(eventId, type, payload));
    }

    public synchronized void failNext(int times) {
        failuresLeft = times;
    }

    public synchronized List<Message> published() {
        return List.copyOf(published);
    }

    public synchronized void clear() {
        published.clear();
        failuresLeft = 0;
    }
}
