package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.store.App;
import dojo.store.Store;
import dojo.store.catalog.internal.ProductRow;
import dojo.store.orders.api.OrderConfirmation;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StoreTest {
    @Test
    void aLightOrderGoesAsALetter() {
        Store store = App.create(List.of(new ProductRow("pen", "Pen", 150, 20, "S-1")));
        assertEquals(new OrderConfirmation("o-1", 450, "letter", 450), store.orders().place("o-1", Map.of("pen", 3)));
    }
}
