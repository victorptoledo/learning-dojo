package dojo;

import static dojo.GradingSupport.code;
import static dojo.GradingSupport.instanceFieldTypes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class OrderStateGradingTest {
    @Test
    void every_illegal_transition_is_refused_with_the_old_message() {
        var created = new Order("a", 100);
        assertRefused("cannot deliver an order that is created", created::deliver);

        var paid = new Order("b", 100);
        paid.pay();
        assertRefused("cannot pay an order that is paid", paid::pay);
        assertRefused("cannot deliver an order that is paid", paid::deliver);

        var shipped = new Order("c", 100);
        shipped.pay();
        shipped.ship("T");
        assertRefused("cannot cancel an order that is shipped", () -> shipped.cancel("late"));
        assertRefused("cannot pay an order that is shipped", shipped::pay);

        var delivered = new Order("d", 100);
        delivered.pay();
        delivered.ship("T");
        delivered.deliver();
        assertRefused("cannot ship an order that is delivered", () -> delivered.ship("T2"));
        assertRefused("cannot cancel an order that is delivered", () -> delivered.cancel("x"));

        var cancelled = new Order("e", 100);
        cancelled.cancel("dup");
        assertRefused("cannot pay an order that is cancelled", cancelled::pay);
        assertRefused("cannot cancel an order that is cancelled", () -> cancelled.cancel("again"));
    }

    @Test
    void a_failed_transition_leaves_the_order_as_it_was() {
        var order = new Order("f", 500);
        order.pay();
        assertThrows(IllegalArgumentException.class, () -> order.ship(" "));
        assertThrows(IllegalStateException.class, order::deliver);
        assertEquals(new Paid(500), order.state());
        assertEquals("PAID", order.status());
    }

    @Test
    void states_carry_only_their_own_data() {
        var order = new Order("g", 4200);
        assertEquals(new Created(4200), order.state());
        order.pay();
        order.ship("TRK-1");
        assertEquals(new Shipped("TRK-1"), order.state());
        order.deliver();
        assertEquals(new Delivered("TRK-1"), order.state());
        assertEquals(List.of(new Created(4200), new Paid(4200), new Shipped("TRK-1"), new Delivered("TRK-1")),
                order.history());
        assertThrows(UnsupportedOperationException.class, () -> order.history().clear());
    }

    @Test
    void cancelling_refunds_only_what_was_paid() {
        var unpaid = new Order("h", 700);
        unpaid.cancel("out of stock");
        assertEquals(new Cancelled("out of stock", 0), unpaid.state());
        assertEquals("h: cancelled (out of stock), refunded 0.00", unpaid.describe());

        var paid = new Order("i", 705);
        paid.pay();
        paid.cancel("fraud");
        assertEquals(new Cancelled("fraud", 705), paid.state());
        assertEquals("i: cancelled (fraud), refunded 7.05", paid.describe());
    }

    @Test
    void transitions_work_on_the_states_alone() {
        OrderState state = new Created(300).pay().ship("X").deliver();
        assertEquals(new Delivered("X"), state);
        assertInstanceOf(Cancelled.class, new Paid(1).cancel("r"));
        assertRefused("cannot deliver an order that is created", () -> new Created(1).deliver());
        assertEquals("delivered", state.name());
    }

    @Test
    void the_state_is_a_sealed_family_of_records() {
        assertTrue(OrderState.class.isInterface() && OrderState.class.isSealed(), "OrderState must be a sealed interface");
        Set<String> permitted = Arrays.stream(OrderState.class.getPermittedSubclasses())
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("Created", "Paid", "Shipped", "Delivered", "Cancelled"), permitted);
        for (Class<?> state : OrderState.class.getPermittedSubclasses()) {
            assertTrue(state.isRecord(), state.getSimpleName() + " should be a record");
        }
    }

    @Test
    void the_order_holds_a_state_not_a_status_string() {
        assertEquals(Set.of(String.class, OrderState.class, List.class), Set.copyOf(instanceFieldTypes(Order.class)),
                "Order should hold its id, its current OrderState and its history, nothing else");
        assertEquals(1, instanceFieldTypes(Order.class).stream().filter(String.class::equals).count(),
                "the only String field left in Order is the id");
        String code = code("dojo.Order");
        assertFalse(code.contains(".equals("), "Order should not compare status strings");
        assertFalse(code.contains("instanceof"), "use a switch over the sealed states instead of instanceof");
        assertFalse(code.contains("default"), "a switch over a sealed interface needs no default: let the compiler check it");
    }

    private static void assertRefused(String message, Runnable transition) {
        var error = assertThrows(IllegalStateException.class, transition::run);
        assertEquals(message, error.getMessage());
    }
}
