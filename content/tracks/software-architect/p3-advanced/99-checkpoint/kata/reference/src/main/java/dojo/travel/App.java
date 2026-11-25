package dojo.travel;

import dojo.travel.adapters.InMemoryTrips;
import dojo.travel.application.TripBookingSaga;
import dojo.travel.domain.TripEvent;
import dojo.travel.messaging.CommandBus;
import dojo.travel.messaging.DeadLetterChannel;
import java.util.function.Consumer;

/** The composition root: the only class that knows the adapters. */
public final class App {
    private App() {}

    public static TripBookingSaga create(CommandBus bus, Consumer<TripEvent> events, DeadLetterChannel deadLetters) {
        return new TripBookingSaga(new InMemoryTrips(), bus, events, deadLetters);
    }
}
