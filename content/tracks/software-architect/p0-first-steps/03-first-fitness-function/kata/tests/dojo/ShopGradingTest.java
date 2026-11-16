package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import dojo.shop.App;
import dojo.shop.application.PlaceOrder;
import dojo.shop.domain.ExchangeRates;
import dojo.shop.domain.Order;
import dojo.shop.domain.OrderLine;
import dojo.shop.domain.PriceConverter;
import dojo.shop.infra.FxApiClient;
import dojo.shop.infra.FxApiRates;
import dojo.shop.infra.OrderRow;
import dojo.shop.infra.OrderTable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShopGradingTest {
    private static final Map<String, Set<String>> SHOP = GradingGraph.scan("dojo.shop");

    @Test
    void theDomainUsesNothingFromApplicationOrInfra() {
        assertTrue(SHOP.containsKey("dojo.shop.domain.PriceConverter"), "the scan found no shop classes");
        List<String> bad = SHOP.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("dojo.shop.domain."))
                .flatMap(entry -> entry.getValue().stream()
                        .filter(used -> used.startsWith("dojo.shop.infra.")
                                || used.startsWith("dojo.shop.application."))
                        .map(used -> entry.getKey() + " -> " + used))
                .toList();
        assertEquals(List.of(), bad, "the domain still uses application or infra");
    }

    @Test
    void exchangeRatesIsAPortInTheDomain() {
        assertTrue(ExchangeRates.class.isInterface(), "ExchangeRates should be an interface");
        assertTrue(ExchangeRates.class.isAssignableFrom(FxApiRates.class),
                "FxApiRates should implement ExchangeRates");
    }

    @Test
    void convertsWithTheRatesItIsGiven() {
        PriceConverter converter = new PriceConverter(currency -> new BigDecimal("1.5"));
        assertEquals(1500, converter.toEuroCents(1000, "CHF"));
    }

    @Test
    void roundsHalfEvenToWholeCents() {
        PriceConverter converter = new PriceConverter(currency -> new BigDecimal("0.17"));
        assertEquals(42, converter.toEuroCents(250, "BRL"));
        assertEquals(26, converter.toEuroCents(150, "BRL"));
    }

    @Test
    void euroIsNeverLookedUp() {
        PriceConverter converter = new PriceConverter(currency -> fail("asked for a rate for " + currency));
        assertEquals(999, converter.toEuroCents(999, "EUR"));
    }

    @Test
    void theAdapterAsksTheClientForThePairToEuro() {
        ExchangeRates rates = new FxApiRates(new FxApiClient());
        assertEquals(new BigDecimal("1.17"), rates.toEuro("GBP"));
        assertThrows(IllegalArgumentException.class, () -> rates.toEuro("JPY"));
    }

    @Test
    void placeOrderStoresTheRowAndReturnsEuros() {
        OrderTable table = new OrderTable();
        PlaceOrder placeOrder = new PlaceOrder(table, new PriceConverter(currency -> new BigDecimal("2")));
        Order order = new Order("o-7", "USD", List.of(
                new OrderLine("pen", 3, 150), new OrderLine("ink", 1, 400)));

        assertEquals(1700, placeOrder.place(order));
        assertEquals(List.of(new OrderRow("o-7", 2, 1700)), table.all());
    }

    @Test
    void theAppWiresTheRealClient() {
        OrderTable table = new OrderTable();
        Order order = new Order("o-8", "GBP", List.of(new OrderLine("map", 1, 1000)));
        assertEquals(1170, App.placeOrder(table).place(order));
        assertEquals(1170, table.find("o-8").orElseThrow().totalEuroCents());
    }
}
