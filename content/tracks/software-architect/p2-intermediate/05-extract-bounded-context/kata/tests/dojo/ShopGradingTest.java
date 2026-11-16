package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.shop.App;
import dojo.shop.Shop;
import dojo.shop.catalog.Product;
import dojo.shop.sales.Order;
import dojo.shop.sales.Products;
import dojo.shop.sales.Quote;
import dojo.shop.sales.SalesService;
import dojo.shop.warehouse.StockItem;
import dojo.shop.warehouse.StockItems;
import dojo.shop.warehouse.WarehouseService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShopGradingTest {
    private final Shop shop = App.create(List.of(
            new Product("mug", "Mug", "A mug", 800, false, 350, "A-1", 5),
            new Product("vase", "Vase", "Old vase", 3000, true, 900, "B-7", 4)));

    @Test
    void quotesAndRepricingStayInSales() {
        assertEquals(new Quote("mug", 3, 2400), shop.sales().quote("mug", 3));
        shop.sales().reprice("mug", 900);
        assertEquals(new Quote("mug", 3, 2700), shop.sales().quote("mug", 3));
        assertEquals(5, shop.warehouse().stock("mug"));
    }

    @Test
    void theLegacyRecordsAreSplitIntoBothContexts() {
        assertEquals("A-1", shop.warehouse().pickLocation("mug"));
        assertEquals("B-7", shop.warehouse().pickLocation("vase"));
        assertEquals(4, shop.warehouse().stock("vase"));
    }

    @Test
    void anOrderReservesStock() {
        assertEquals(new Order("o-1", "mug", 5, 4000), shop.sales().placeOrder("o-1", "mug", 5));
        assertEquals(0, shop.warehouse().stock("mug"));
        IllegalStateException out = assertThrows(IllegalStateException.class,
                () -> shop.sales().placeOrder("o-2", "mug", 1));
        assertEquals("out of stock: mug", out.getMessage());
    }

    @Test
    void receivingRaisesStock() {
        shop.warehouse().receive("mug", 10);
        assertEquals(15, shop.warehouse().stock("mug"));
        assertThrows(IllegalArgumentException.class, () -> shop.warehouse().receive("mug", 0));
        assertThrows(IllegalArgumentException.class, () -> shop.warehouse().receive("nope", 1));
    }

    @Test
    void badOrdersTouchNoStock() {
        IllegalStateException discontinued = assertThrows(IllegalStateException.class,
                () -> shop.sales().placeOrder("o-3", "vase", 1));
        assertEquals("not for sale: vase", discontinued.getMessage());
        assertThrows(IllegalArgumentException.class, () -> shop.sales().placeOrder("o-4", "nope", 1));
        assertThrows(IllegalArgumentException.class, () -> shop.sales().placeOrder("o-5", "mug", 0));
        assertEquals(4, shop.warehouse().stock("vase"));
        assertEquals(5, shop.warehouse().stock("mug"));
    }

    @Test
    void salesWorksWithAnyStock() {
        Products products = new Products();
        products.save(new dojo.shop.sales.Product("pen", "Pen", 150, false));
        List<String> asked = new ArrayList<>();
        SalesService sales = new SalesService(products, (sku, quantity) -> asked.add(sku + " x" + quantity));

        assertEquals(new Order("o-1", "pen", 4, 600), sales.placeOrder("o-1", "pen", 4));
        assertEquals(List.of("pen x4"), asked);
    }

    @Test
    void reserveRefusesWithoutChangingAnything() {
        StockItems items = new StockItems();
        items.save(new StockItem("pen", 10, "C-2", 3));
        WarehouseService warehouse = new WarehouseService(items);

        assertFalse(warehouse.reserve("pen", 4));
        assertFalse(warehouse.reserve("nope", 1));
        assertEquals(3, warehouse.stock("pen"));
        assertTrue(warehouse.reserve("pen", 3));
        assertEquals(new StockItem("pen", 10, "C-2", 0), items.find("pen").orElseThrow());
    }
}
