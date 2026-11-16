package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.fitness.ClassGraph;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    private static final Map<String, Set<String>> RENTALS = ClassGraph.scan("dojo.rentals");

    private static List<String> uses(String from, Predicate<String> forbidden) {
        return RENTALS.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(from))
                .flatMap(entry -> entry.getValue().stream().filter(forbidden).map(to -> entry.getKey() + " -> " + to))
                .toList();
    }

    @Test
    void onlyTheAntiCorruptionLayerSpeaksToTheCrm() {
        assertEquals(List.of(), uses("dojo.rentals.booking.domain.", to -> to.startsWith("dojo.legacy.")));
    }

    @Test
    void bookingUsesOnlyBillingsApi() {
        assertEquals(List.of(), uses("dojo.rentals.booking.", to -> to.startsWith("dojo.rentals.billing.")
                && !to.startsWith("dojo.rentals.billing.api.")));
    }
}
