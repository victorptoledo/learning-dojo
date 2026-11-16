package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.shop.billing.InvoiceService;
import dojo.shop.billing.PaymentListener;
import dojo.shop.billing.PaymentService;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CycleGradingTest {
    private static final Map<String, Set<String>> PACKAGES =
            GradingGraph.packageDependencies(GradingGraph.scan("dojo.shop"));

    @Test
    void billingUsesNothingInOrders() {
        assertTrue(PACKAGES.containsKey("dojo.shop.billing"), "the scan found no billing classes");
        assertFalse(PACKAGES.get("dojo.shop.billing").contains("dojo.shop.orders"),
                "billing still uses orders: " + PACKAGES);
    }

    @Test
    void ordersStillUseBilling() {
        assertTrue(PACKAGES.get("dojo.shop.orders").contains("dojo.shop.billing"),
                "keep the direction orders -> billing");
    }

    @Test
    void noPackageReachesItself() {
        for (String start : PACKAGES.keySet()) {
            Set<String> seen = new HashSet<>();
            Deque<String> todo = new ArrayDeque<>(PACKAGES.get(start));
            while (!todo.isEmpty()) {
                String next = todo.pop();
                assertFalse(next.equals(start), "package cycle through " + start + ": " + PACKAGES);
                if (seen.add(next)) {
                    todo.addAll(PACKAGES.getOrDefault(next, Set.of()));
                }
            }
        }
    }

    @Test
    void paymentListenerIsAPortOfBilling() {
        assertTrue(PaymentListener.class.isInterface(), "PaymentListener should be an interface");
        assertEquals(List.of("void invoicePaid(String)"),
                java.util.Arrays.stream(PaymentListener.class.getMethods())
                        .map(m -> m.getReturnType().getSimpleName() + " " + m.getName() + "("
                                + String.join(",", java.util.Arrays.stream(m.getParameterTypes())
                                        .map(Class::getSimpleName).toList()) + ")")
                        .toList());
    }

    @Test
    void billingWorksWithAnyListener() {
        InvoiceService invoices = new InvoiceService();
        List<String> told = new java.util.ArrayList<>();
        PaymentService payments = new PaymentService(invoices, told::add);
        invoices.invoice(new dojo.shop.billing.InvoiceRequest("o-9", "bob", 1000));

        payments.recordPayment("INV-1", 1200);

        assertEquals(List.of("o-9"), told);
    }
}
