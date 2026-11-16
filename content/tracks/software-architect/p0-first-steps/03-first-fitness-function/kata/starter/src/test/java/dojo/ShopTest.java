package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.shop.App;
import dojo.shop.domain.Order;
import dojo.shop.domain.OrderLine;
import dojo.shop.infra.OrderRow;
import dojo.shop.infra.OrderTable;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShopTest {
    @Test
    void anOrderInDollarsIsStoredInEuros() {
        OrderTable table = new OrderTable();
        Order order = new Order("o-1", "USD", List.of(new OrderLine("book", 2, 1000)));

        long euros = App.placeOrder(table).place(order);

        assertEquals(1840, euros);
        assertEquals(List.of(new OrderRow("o-1", 1, 1840)), table.all());
    }
}
