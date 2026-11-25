package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.travel.App;
import dojo.travel.application.TripBookingSaga;
import dojo.travel.domain.Leg;
import dojo.travel.domain.TripCancelled;
import dojo.travel.domain.TripConfirmed;
import dojo.travel.domain.TripEvent;
import dojo.travel.domain.TripId;
import dojo.travel.messaging.SupplierCommand;
import dojo.travel.messaging.SupplierReply;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TripSagaTest {
    private final List<SupplierCommand> sent = new ArrayList<>();
    private final List<TripEvent> events = new ArrayList<>();
    private final List<String> deadLetters = new ArrayList<>();
    private final TripBookingSaga saga =
            App.create(sent::add, events::add, (reply, reason) -> deadLetters.add(reply.messageId() + ": " + reason));

    @Test
    void reservesEveryLegInTurnAndConfirms() {
        saga.start("t1", "Ada", List.of(Leg.FLIGHT, Leg.HOTEL));
        saga.handle(SupplierReply.reserved("m1", "t1", Leg.FLIGHT, "FL-9"));
        saga.handle(SupplierReply.reserved("m2", "t1", Leg.HOTEL, "HT-4"));

        assertEquals(List.of(SupplierCommand.reserve("t1", Leg.FLIGHT), SupplierCommand.reserve("t1", Leg.HOTEL)), sent);
        assertEquals(List.of(new TripConfirmed(new TripId("t1"))), events);
    }

    @Test
    void aRefusalReleasesWhatWasReserved() {
        saga.start("t2", "Grace", List.of(Leg.FLIGHT, Leg.HOTEL, Leg.CAR));
        saga.handle(SupplierReply.reserved("m1", "t2", Leg.FLIGHT, "FL-1"));
        saga.handle(SupplierReply.refused("m2", "t2", Leg.HOTEL, "fully booked"));

        assertEquals(SupplierCommand.release("t2", Leg.FLIGHT), sent.getLast());
        assertEquals(List.of(new TripCancelled(new TripId("t2"), "HOTEL refused: fully booked")), events);
    }

    @Test
    void aRepeatedReplyIsIgnored() {
        saga.start("t3", "Linus", List.of(Leg.FLIGHT, Leg.CAR));
        SupplierReply flight = SupplierReply.reserved("m1", "t3", Leg.FLIGHT, "FL-2");
        saga.handle(flight);
        saga.handle(flight);

        assertEquals(2, sent.size(), "the car is asked for once");
        assertEquals(List.of(), deadLetters);
    }
}
