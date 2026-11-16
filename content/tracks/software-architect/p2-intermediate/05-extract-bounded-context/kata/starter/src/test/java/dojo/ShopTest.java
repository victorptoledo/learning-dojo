package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.shop.App;
import dojo.shop.Shop;
import dojo.shop.catalog.Product;
import dojo.shop.sales.Order;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShopTest {
    @Test
    void anOrderTakesStockFromTheWarehouse() {
        Shop shop = App.create(List.of(new Product("mug", "Mug", "A mug", 800, false, 350, "A-1", 5)));

        assertEquals(new Order("o-1", "mug", 2, 1600), shop.sales().placeOrder("o-1", "mug", 2));
        assertEquals(3, shop.warehouse().stock("mug"));
    }
}
