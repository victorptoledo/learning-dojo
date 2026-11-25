package dojo;

import static dojo.travel.domain.Leg.CAR;
import static dojo.travel.domain.Leg.FLIGHT;
import static dojo.travel.domain.Leg.HOTEL;
import static dojo.travel.messaging.SupplierCommand.release;
import static dojo.travel.messaging.SupplierCommand.reserve;
import static dojo.travel.messaging.SupplierReply.refused;
import static dojo.travel.messaging.SupplierReply.reserved;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dojo.travel.App;
import dojo.travel.adapters.InMemoryTrips;
import dojo.travel.application.TripBookingSaga;
import dojo.travel.domain.Leg;
import dojo.travel.domain.Trip;
import dojo.travel.domain.TripCancelled;
import dojo.travel.domain.TripConfirmed;
import dojo.travel.domain.TripEvent;
import dojo.travel.domain.TripId;
import dojo.travel.domain.TripStatus;
import dojo.travel.domain.Trips;
import dojo.travel.messaging.SupplierCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TripSagaGradingTest {
    private final List<SupplierCommand> sent = new ArrayList<>();
    private final List<TripEvent> events = new ArrayList<>();
    private final List<String> deadLetters = new ArrayList<>();
    private final Trips trips = new InMemoryTrips();
    private final TripBookingSaga saga = new TripBookingSaga(trips, sent::add, events::add,
            (reply, reason) -> deadLetters.add(reply.messageId() + ": " + reason));

    private Trip trip(String id) {
        return trips.find(new TripId(id)).orElseThrow(() -> new AssertionError("trip " + id + " was not saved"));
    }

    private static TripCancelled cancelled(String id, String reason) {
        return new TripCancelled(new TripId(id), reason);
    }

    private void startAll(String id) {
        saga.start(id, "Ada", List.of(FLIGHT, HOTEL, CAR));
    }

    @Test
    void startSavesThePendingTripAndAsksForTheFirstLeg() {
        TripId id = saga.start("t1", "Ada", List.of(HOTEL, CAR));
        assertEquals(new TripId("t1"), id);
        assertEquals(TripStatus.PENDING, trip("t1").status());
        assertEquals(List.of(reserve("t1", HOTEL)), sent);
        assertEquals(List.of(), events);
    }

    @Test
    void startRefusesATripThatExistsOrIsInvalid() {
        saga.start("t1", "Ada", List.of(CAR));
        IllegalArgumentException twice = assertThrows(IllegalArgumentException.class,
                () -> saga.start("t1", "Grace", List.of(FLIGHT)));
        assertEquals("trip exists: t1", twice.getMessage());
        assertThrows(IllegalArgumentException.class, () -> saga.start("t2", "Grace", List.of()));
        assertEquals(List.of(reserve("t1", CAR)), sent, "a refused start sends nothing");
        assertEquals(Optional.empty(), trips.find(new TripId("t2")));
    }

    @Test
    void theHappyPathReservesInOrderAndConfirmsOnce() {
        startAll("t1");
        saga.handle(reserved("m1", "t1", FLIGHT, "FL-1"));
        saga.handle(reserved("m2", "t1", HOTEL, "HT-2"));
        saga.handle(reserved("m3", "t1", CAR, "CR-3"));
        assertEquals(List.of(reserve("t1", FLIGHT), reserve("t1", HOTEL), reserve("t1", CAR)), sent);
        assertEquals(List.of(new TripConfirmed(new TripId("t1"))), events);
        Trip trip = trip("t1");
        assertEquals(TripStatus.CONFIRMED, trip.status());
        assertEquals(Optional.of("HT-2"), trip.confirmation(HOTEL));
    }

    @Test
    void aRefusedFirstLegCancelsWithoutReleases() {
        startAll("t1");
        saga.handle(refused("m1", "t1", FLIGHT, "no seats"));
        assertEquals(List.of(reserve("t1", FLIGHT)), sent);
        assertEquals(List.of(cancelled("t1", "FLIGHT refused: no seats")), events);
        assertEquals(TripStatus.CANCELLED, trip("t1").status());
    }

    @Test
    void aRefusedLastLegReleasesTheOthersLatestFirst() {
        startAll("t1");
        saga.handle(reserved("m1", "t1", FLIGHT, "FL-1"));
        saga.handle(reserved("m2", "t1", HOTEL, "HT-2"));
        saga.handle(refused("m3", "t1", CAR, "no cars"));
        assertEquals(List.of(release("t1", HOTEL), release("t1", FLIGHT)), sent.subList(3, sent.size()));
        assertEquals(List.of(cancelled("t1", "CAR refused: no cars")), events);
    }

    @Test
    void aRepeatedReservationIsProcessedOnce() {
        startAll("t1");
        saga.handle(reserved("m1", "t1", FLIGHT, "FL-1"));
        saga.handle(reserved("m1", "t1", FLIGHT, "FL-1"));
        assertEquals(List.of(reserve("t1", FLIGHT), reserve("t1", HOTEL)), sent);
        assertEquals(List.of(), deadLetters, "a duplicate is not an error");
    }

    @Test
    void aRepeatedRefusalReleasesOnce() {
        startAll("t1");
        saga.handle(reserved("m1", "t1", FLIGHT, "FL-1"));
        saga.handle(refused("m2", "t1", HOTEL, "full"));
        saga.handle(refused("m2", "t1", HOTEL, "full"));
        assertEquals(List.of(reserve("t1", FLIGHT), reserve("t1", HOTEL), release("t1", FLIGHT)), sent);
        assertEquals(1, events.size());
        assertEquals(List.of(), deadLetters);
    }

    @Test
    void aReplyForAnUnknownTripIsDeadLettered() {
        saga.handle(reserved("m9", "ghost", FLIGHT, "FL-1"));
        assertEquals(List.of("m9: unknown trip"), deadLetters);
        assertEquals(List.of(), sent);
    }

    @Test
    void aReplyForTheWrongLegIsDeadLetteredAndChangesNothing() {
        startAll("t1");
        saga.handle(reserved("m1", "t1", CAR, "CR-1"));
        saga.handle(refused("m2", "t1", HOTEL, "full"));
        assertEquals(List.of("m1: unexpected reply for CAR", "m2: unexpected reply for HOTEL"), deadLetters);
        assertEquals(List.of(reserve("t1", FLIGHT)), sent);
        assertEquals(TripStatus.PENDING, trip("t1").status());
        assertEquals(Optional.of(FLIGHT), trip("t1").nextLeg());
        assertEquals(List.of(), events);
    }

    @Test
    void aDeadLetteredReplyIsNotDeadLetteredTwice() {
        saga.handle(reserved("m9", "ghost", FLIGHT, "FL-1"));
        saga.handle(reserved("m9", "ghost", FLIGHT, "FL-1"));
        assertEquals(List.of("m9: unknown trip"), deadLetters);
    }

    @Test
    void aReplyForAConfirmedTripIsDeadLettered() {
        saga.start("t1", "Ada", List.of(CAR));
        saga.handle(reserved("m1", "t1", CAR, "CR-1"));
        saga.handle(reserved("m2", "t1", CAR, "CR-2"));
        assertEquals(List.of("m2: trip is confirmed"), deadLetters);
        assertEquals(List.of(reserve("t1", CAR)), sent);
    }

    @Test
    void theTravellerCanCancelWhileALegIsPending() {
        startAll("t1");
        saga.handle(reserved("m1", "t1", FLIGHT, "FL-1"));
        saga.cancel("t1", "changed plans");
        assertEquals(List.of(reserve("t1", FLIGHT), reserve("t1", HOTEL), release("t1", FLIGHT)), sent);
        assertEquals(List.of(cancelled("t1", "changed plans")), events);
        assertEquals(TripStatus.CANCELLED, trip("t1").status());
    }

    @Test
    void aReservationThatArrivesAfterTheCancellationIsReleased() {
        startAll("t1");
        saga.handle(reserved("m1", "t1", FLIGHT, "FL-1"));
        saga.cancel("t1", "changed plans");
        saga.handle(reserved("m2", "t1", HOTEL, "HT-2"));
        assertEquals(release("t1", HOTEL), sent.getLast(), "otherwise the hotel room stays booked forever");
        assertEquals(4, sent.size());
        assertEquals(List.of(), deadLetters);
        assertEquals(1, events.size(), "the trip is not cancelled twice");
    }

    @Test
    void aLateRefusalOrAnAlreadyReleasedLegAfterTheCancellationDoesNothing() {
        startAll("t1");
        saga.handle(reserved("m1", "t1", FLIGHT, "FL-1"));
        saga.cancel("t1", "changed plans");
        int before = sent.size();
        saga.handle(refused("m2", "t1", HOTEL, "full"));
        saga.handle(reserved("m3", "t1", FLIGHT, "FL-1"));
        assertEquals(before, sent.size());
        assertEquals(List.of(), deadLetters);
    }

    @Test
    void cancellingAnUnknownOrConfirmedTripIsRefused() {
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class, () -> saga.cancel("nope", "x"));
        assertEquals("unknown trip nope", unknown.getMessage());
        saga.start("t1", "Ada", List.of(CAR));
        saga.handle(reserved("m1", "t1", CAR, "CR-1"));
        assertThrows(IllegalStateException.class, () -> saga.cancel("t1", "too late"));
        assertEquals(List.of(reserve("t1", CAR)), sent);
        assertEquals(1, events.size());
    }

    @Test
    void tripsAreIndependent() {
        saga.start("a", "Ada", List.of(FLIGHT, CAR));
        saga.start("b", "Bob", List.of(FLIGHT, CAR));
        saga.handle(reserved("m1", "a", FLIGHT, "FL-1"));
        saga.handle(refused("m2", "b", FLIGHT, "no seats"));
        saga.handle(reserved("m3", "a", CAR, "CR-1"));
        assertEquals(TripStatus.CONFIRMED, trip("a").status());
        assertEquals(TripStatus.CANCELLED, trip("b").status());
        assertEquals(List.of(cancelled("b", "FLIGHT refused: no seats"), new TripConfirmed(new TripId("a"))), events);
    }

    @Test
    void theCompositionRootWiresTheSaga() {
        List<SupplierCommand> bus = new ArrayList<>();
        List<TripEvent> published = new ArrayList<>();
        List<String> dead = new ArrayList<>();
        TripBookingSaga wired = App.create(bus::add, published::add, (reply, reason) -> dead.add(reason));
        wired.start("t1", "Ada", List.of(Leg.CAR));
        wired.handle(reserved("m1", "t1", Leg.CAR, "CR-1"));
        wired.handle(reserved("m2", "zzz", Leg.CAR, "CR-1"));
        assertEquals(List.of(reserve("t1", Leg.CAR)), bus);
        assertEquals(List.of(new TripConfirmed(new TripId("t1"))), published);
        assertEquals(List.of("unknown trip"), dead);
    }
}
