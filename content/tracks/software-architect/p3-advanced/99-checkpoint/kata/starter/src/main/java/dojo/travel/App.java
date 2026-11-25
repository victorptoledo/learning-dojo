package dojo.travel;

import dojo.travel.adapters.InMemoryTrips;
import dojo.travel.application.TripBookingSaga;
import dojo.travel.messaging.CommandBus;

/** The composition root. */
public final class App {
    private App() {}

    public static TripBookingSaga create(CommandBus bus) {
        return new TripBookingSaga(new InMemoryTrips(), bus);
    }
}
