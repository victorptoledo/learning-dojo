package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FreightQuoterTest {
    private static Carrier fixed(String name, long priceCents, int days) {
        return new Carrier() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Quote quote(Shipment shipment) {
                return new Quote(name, priceCents, days);
            }
        };
    }

    @Test
    void cheapest_picks_the_lowest_price() {
        var slow = new Quote("Slowpost", 900, 6);
        var fast = new Quote("Rapido", 1500, 1);
        assertEquals(Optional.of(slow), Choice.cheapest().pick(List.of(fast, slow)));
    }

    @Test
    void quotes_every_carrier_in_order() {
        var quoter = new FreightQuoter(
                List.of(fixed("Rapido", 1500, 1), fixed("Slowpost", 900, 6)), 2, Duration.ofSeconds(2));
        var board = quoter.quote(new Shipment("o-1", "Lisbon", "Porto", 3), Choice.fastest());
        assertEquals(List.of(
                new QuoteResult.Quoted(new Quote("Rapido", 1500, 1)),
                new QuoteResult.Quoted(new Quote("Slowpost", 900, 6))), board.results());
        assertEquals("Rapido", board.best().orElseThrow().carrier());
    }
}
