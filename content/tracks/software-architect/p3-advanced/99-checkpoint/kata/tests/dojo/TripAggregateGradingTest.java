package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.travel.domain.Leg;
import dojo.travel.domain.Trip;
import dojo.travel.domain.TripCancelled;
import dojo.travel.domain.TripConfirmed;
import dojo.travel.domain.TripId;
import dojo.travel.domain.TripStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TripAggregateGradingTest {
    private static final TripId ID = new TripId("t-1");

    private static Trip trip(Leg... legs) {
        return Trip.request(ID, "Ada", List.of(legs));
    }

    @Test
    void aTripIdIsNeverBlank() {
        assertThrows(IllegalArgumentException.class, () -> new TripId(" "));
        assertThrows(IllegalArgumentException.class, () -> new TripId(null));
        assertEquals(new TripId("a"), new TripId("a"), "a value object is equal by value");
    }

    @Test
    void aRequestNeedsATravellerAndDistinctLegs() {
        assertThrows(IllegalArgumentException.class, () -> Trip.request(ID, " ", List.of(Leg.CAR)));
        assertThrows(IllegalArgumentException.class, () -> Trip.request(ID, "Ada", List.of()));
        assertThrows(IllegalArgumentException.class, () -> Trip.request(ID, "Ada", List.of(Leg.CAR, Leg.CAR)));
    }

    @Test
    void aNewTripIsPendingAndWaitsForItsFirstLeg() {
        Trip trip = trip(Leg.HOTEL, Leg.FLIGHT);
        assertEquals(ID, trip.id());
        assertEquals("Ada", trip.traveller());
        assertEquals(TripStatus.PENDING, trip.status());
        assertEquals(List.of(Leg.HOTEL, Leg.FLIGHT), trip.legs(), "legs are booked in the order requested");
        assertEquals(Optional.of(Leg.HOTEL), trip.nextLeg());
        assertEquals(Optional.empty(), trip.confirmation(Leg.HOTEL));
        assertEquals(List.of(), trip.pullEvents());
    }

    @Test
    void theRequestedLegsCannotBeChangedFromOutside() {
        List<Leg> legs = new ArrayList<>(List.of(Leg.FLIGHT));
        Trip trip = Trip.request(ID, "Ada", legs);
        legs.add(Leg.CAR);
        assertEquals(List.of(Leg.FLIGHT), trip.legs(), "the trip keeps its own copy");
        assertThrows(UnsupportedOperationException.class, () -> trip.legs().add(Leg.HOTEL));
    }

    @Test
    void reservingTheLastLegConfirmsTheTripOnce() {
        Trip trip = trip(Leg.FLIGHT, Leg.CAR);
        trip.reserved(Leg.FLIGHT, "FL-1");
        assertEquals(Optional.of(Leg.CAR), trip.nextLeg());
        assertEquals(List.of(), trip.pullEvents(), "nothing happened yet for the outside world");
        trip.reserved(Leg.CAR, "CR-7");
        assertEquals(TripStatus.CONFIRMED, trip.status());
        assertEquals(Optional.of("CR-7"), trip.confirmation(Leg.CAR));
        assertEquals(Optional.empty(), trip.nextLeg());
        assertEquals(List.of(new TripConfirmed(ID)), trip.pullEvents());
        assertEquals(List.of(), trip.pullEvents(), "pulled events are forgotten");
    }

    @Test
    void onlyTheNextLegCanBeReservedAndARefusalChangesNothing() {
        Trip trip = trip(Leg.FLIGHT, Leg.HOTEL, Leg.CAR);
        IllegalStateException wrong = assertThrows(IllegalStateException.class, () -> trip.reserved(Leg.HOTEL, "HT-1"));
        assertEquals("expected FLIGHT, got HOTEL", wrong.getMessage());
        assertThrows(IllegalArgumentException.class, () -> trip.reserved(Leg.FLIGHT, " "));
        assertEquals(Optional.empty(), trip.confirmation(Leg.HOTEL));
        assertEquals(Optional.empty(), trip.confirmation(Leg.FLIGHT));
        assertEquals(Optional.of(Leg.FLIGHT), trip.nextLeg());
        assertEquals(TripStatus.PENDING, trip.status());
    }

    @Test
    void cancellingReturnsTheReservedLegsLatestFirst() {
        Trip trip = trip(Leg.FLIGHT, Leg.HOTEL, Leg.CAR);
        trip.reserved(Leg.FLIGHT, "FL-1");
        trip.reserved(Leg.HOTEL, "HT-2");
        assertEquals(List.of(Leg.HOTEL, Leg.FLIGHT), trip.cancel("changed plans"));
        assertEquals(TripStatus.CANCELLED, trip.status());
        assertEquals(Optional.empty(), trip.nextLeg());
        assertEquals(List.of(new TripCancelled(ID, "changed plans")), trip.pullEvents());
    }

    @Test
    void cancellingBeforeAnyReservationReleasesNothing() {
        assertEquals(List.of(), trip(Leg.CAR).cancel("no car needed"));
    }

    @Test
    void onlyAPendingTripAcceptsCommands() {
        Trip confirmed = trip(Leg.CAR);
        confirmed.reserved(Leg.CAR, "CR-1");
        IllegalStateException late = assertThrows(IllegalStateException.class, () -> confirmed.cancel("too late"));
        assertEquals("trip is CONFIRMED", late.getMessage());

        Trip cancelled = trip(Leg.FLIGHT);
        cancelled.cancel("sick");
        cancelled.pullEvents();
        IllegalStateException again = assertThrows(IllegalStateException.class, () -> cancelled.reserved(Leg.FLIGHT, "FL-1"));
        assertEquals("trip is CANCELLED", again.getMessage());
        assertThrows(IllegalStateException.class, () -> cancelled.cancel("twice"));
        assertEquals(List.of(), cancelled.pullEvents(), "a refused command records nothing");
    }

    @Test
    void aCancellationNeedsAReason() {
        Trip trip = trip(Leg.FLIGHT);
        assertThrows(IllegalArgumentException.class, () -> trip.cancel(" "));
        assertTrue(trip.status() == TripStatus.PENDING, "a refused cancellation changes nothing");
    }
}
