package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.travel.domain.Trip;
import dojo.travel.domain.TripCancelled;
import dojo.travel.domain.TripConfirmed;
import dojo.travel.domain.TripEvent;
import dojo.travel.domain.TripId;
import dojo.travel.domain.Trips;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class TravelArchitectureGradingTest {
    private static final Map<String, Set<String>> TRAVEL = GradingGraph.scan("dojo.travel");

    private static List<String> uses(Predicate<String> from, Predicate<String> forbidden) {
        return TRAVEL.entrySet().stream()
                .filter(entry -> from.test(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream().filter(forbidden).map(to -> entry.getKey() + " -> " + to))
                .toList();
    }

    private static Predicate<String> in(String pkg) {
        return name -> name.startsWith("dojo.travel." + pkg + ".");
    }

    @Test
    void theDomainUsesNothingElseOfTheApp() {
        assertTrue(TRAVEL.containsKey("dojo.travel.domain.Trip"), "the scan found no travel classes");
        assertEquals(List.of(), uses(in("domain"), in("domain").negate().and(name -> name.startsWith("dojo.travel."))));
    }

    @Test
    void messagesKnowOnlyTheDomain() {
        assertEquals(List.of(), uses(in("messaging"),
                in("application").or(in("adapters")).or(name -> name.equals("dojo.travel.App"))));
    }

    @Test
    void onlyTheCompositionRootKnowsTheAdapters() {
        assertEquals(List.of(), uses(in("adapters").negate().and(name -> !name.equals("dojo.travel.App")), in("adapters")));
    }

    @Test
    void thePackagesHaveNoCycle() {
        Map<String, Set<String>> packages = GradingGraph.packageDependencies(TRAVEL);
        for (String start : packages.keySet()) {
            Set<String> seen = new HashSet<>();
            Deque<String> todo = new ArrayDeque<>(packages.get(start));
            while (!todo.isEmpty()) {
                String next = todo.pop();
                assertTrue(!next.equals(start), "package cycle through " + start);
                if (seen.add(next)) {
                    todo.addAll(packages.getOrDefault(next, Set.of()));
                }
            }
        }
    }

    @Test
    void tripIsAnAggregateRootWithNoWayInButItsCommands() {
        assertTrue(Modifier.isFinal(Trip.class.getModifiers()), "Trip should be final");
        assertEquals(0, Trip.class.getConstructors().length, "Trip should have no public constructor");
        List<String> setters = Arrays.stream(Trip.class.getMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .toList();
        assertEquals(List.of(), setters, "Trip should have no setters");
        assertTrue(Arrays.stream(Trip.class.getDeclaredFields()).allMatch(field -> Modifier.isPrivate(field.getModifiers())),
                "Trip's fields should be private");
    }

    @Test
    void theValueObjectsAndEventsAreRecordsAndTheEventsAreSealed() {
        assertTrue(TripId.class.isRecord(), "TripId should be a record");
        assertTrue(TripEvent.class.isSealed(), "TripEvent should be sealed");
        assertEquals(Set.of(TripConfirmed.class, TripCancelled.class), Set.of(TripEvent.class.getPermittedSubclasses()));
        assertTrue(TripConfirmed.class.isRecord() && TripCancelled.class.isRecord(), "the events should be records");
        assertTrue(Trips.class.isInterface(), "Trips should be the repository port");
    }
}
