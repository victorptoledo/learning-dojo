package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/** Each test starts with an empty database, fresh fake services and the flag off. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CheckoutGradingTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    CheckoutSaga saga;

    @Autowired
    SagaRepository sagas;

    @Autowired
    OrderRepository orders;

    @Autowired
    FakeInventory inventory;

    @Autowired
    FakePayments payments;

    @Autowired
    FakeShipping shipping;

    @Autowired
    FlagStore flags;

    @Autowired
    Rollout rollout;

    @Autowired
    LegacyCheckout legacy;

    @Autowired
    OrderBackfill backfill;

    static CheckoutRequest request(String sku, int quantity, String address) {
        return new CheckoutRequest("Ada Lovelace", sku, quantity, 2500L * quantity, address);
    }

    SagaRecord onlySaga() {
        List<SagaRecord> all = sagas.findAll();
        assertEquals(1, all.size(), "one saga record per checkout");
        return all.getFirst();
    }

    ShopOrder order(long id) {
        return orders.findById(id).orElseThrow();
    }

    // ---- The saga -----------------------------------------------------------------------------------

    @Test
    void a_checkout_reserves_charges_ships_and_confirms() {
        CheckoutResult result = saga.start(request("BOOK", 3, "1 Main St"));
        assertEquals(OrderStatus.CONFIRMED, result.status());
        assertEquals("saga", result.path());
        SagaRecord record = onlySaga();
        assertEquals(SagaStep.DONE, record.getStep());
        assertEquals(result.orderId(), record.getOrderId());
        assertEquals(97, inventory.stock("BOOK"));
        assertEquals(7500, payments.capturedCents());
        assertTrue(shipping.scheduled(record.getId()), "every service gets the saga id as its idempotency key");
        ShopOrder order = order(result.orderId());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals("Ada Lovelace", order.getCustomerName());
        assertEquals("Ada Lovelace", order.getCustomer(), "the new code writes both columns during the migration");
    }

    @Test
    void out_of_stock_cancels_without_charging() {
        CheckoutResult result = saga.start(request("LAMP", 3, "1 Main St"));
        assertEquals(OrderStatus.CANCELLED, result.status());
        assertEquals("out of stock", result.reason());
        assertEquals(0, payments.chargeCalls());
        assertEquals(2, inventory.stock("LAMP"));
        assertEquals(SagaStep.COMPENSATED, onlySaga().getStep());
        assertEquals("out of stock", order(result.orderId()).getReason());
    }

    @Test
    void a_declined_card_releases_the_stock() {
        payments.declineNext();
        CheckoutResult result = saga.start(request("LAMP", 2, "1 Main St"));
        assertEquals(OrderStatus.CANCELLED, result.status());
        assertEquals("card declined", result.reason());
        assertEquals(2, inventory.stock("LAMP"));
        assertFalse(inventory.reserved(onlySaga().getId()));
        assertEquals(0, payments.capturedCents());
        assertEquals(OrderStatus.CANCELLED, order(result.orderId()).getStatus());
    }

    @Test
    void an_undeliverable_address_refunds_and_releases() {
        CheckoutResult result = saga.start(request("BOOK", 1, "7 Coral Way, Atlantis"));
        assertEquals(OrderStatus.CANCELLED, result.status());
        assertEquals("undeliverable", result.reason());
        SagaRecord record = onlySaga();
        assertTrue(payments.refunded(record.getId()));
        assertEquals(0, payments.capturedCents());
        assertEquals(100, inventory.stock("BOOK"));
        assertEquals(0, shipping.scheduledCount());
        assertEquals(SagaStep.SHIPPING, record.getFailedAt());
    }

    @Test
    void a_crash_after_the_charge_leaves_a_saga_that_resumes_without_charging_twice() {
        payments.crashAfterNextCharge();
        assertThrows(CrashException.class, () -> saga.start(request("BOOK", 1, "1 Main St")));

        SagaRecord record = onlySaga();
        assertEquals(SagaStep.CHARGING, record.getStep(), "each step commits on its own; no transaction spans the saga");
        assertEquals(OrderStatus.PENDING, order(record.getOrderId()).getStatus());

        assertEquals(1, saga.resumeStuck());
        assertEquals(2, payments.chargeCalls(), "the charge is sent again with the same key");
        assertEquals(2500, payments.capturedCents(), "and the payment service charges once");
        assertEquals(99, inventory.stock("BOOK"));
        assertEquals(1, shipping.scheduledCount());
        assertEquals(OrderStatus.CONFIRMED, order(record.getOrderId()).getStatus());
        assertEquals(SagaStep.DONE, onlySaga().getStep());
        assertEquals(0, saga.resumeStuck(), "a finished saga is not stuck");
    }

    @Test
    void a_crash_while_compensating_is_finished_by_the_next_run() {
        inventory.crashOnNextRelease();
        assertThrows(CrashException.class, () -> saga.start(request("BOOK", 4, "Atlantis")));
        SagaRecord record = onlySaga();
        assertEquals(SagaStep.COMPENSATING, record.getStep());
        assertEquals("undeliverable", record.getReason());

        assertEquals(1, saga.resumeStuck());
        assertEquals(100, inventory.stock("BOOK"), "the stock comes back exactly once");
        assertEquals(0, payments.capturedCents());
        ShopOrder order = order(record.getOrderId());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("undeliverable", order.getReason());
        assertEquals(SagaStep.COMPENSATED, onlySaga().getStep());
    }

    @Test
    void resuming_a_finished_saga_only_reports_it() {
        CheckoutResult first = saga.start(request("BOOK", 1, "1 Main St"));
        String id = onlySaga().getId();
        assertEquals(first, saga.resume(id));
        assertEquals(1, payments.chargeCalls());
        assertEquals(99, inventory.stock("BOOK"));
    }

    // ---- Expand and contract ------------------------------------------------------------------------

    @Test
    void the_monolith_writes_both_columns_too() {
        CheckoutResult result = legacy.checkout(request("BOOK", 1, "1 Main St"));
        assertEquals("legacy", result.path());
        ShopOrder order = order(result.orderId());
        assertEquals("Ada Lovelace", order.getCustomerName());
        assertEquals("Ada Lovelace", order.getCustomer(), "a writer that skips the new column keeps the backfill going forever");
    }

    @Test
    void the_backfill_copies_old_rows_in_batches_lowest_ids_first_and_never_overwrites() {
        List<Long> old = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            old.add(orders.save(new ShopOrder("Customer " + i, null, "BOOK", 1, 100, OrderStatus.CONFIRMED)).getId());
        }
        long both = orders.save(new ShopOrder("Old Name", "New Name", "BOOK", 1, 100, OrderStatus.CONFIRMED)).getId();
        assertEquals(5, backfill.remaining());

        assertEquals(2, backfill.runBatch(2));
        assertEquals("Customer 1", order(old.get(0)).getCustomer());
        assertEquals("Customer 2", order(old.get(1)).getCustomer());
        assertNull(order(old.get(2)).getCustomer(), "lowest ids first");
        assertEquals(3, backfill.remaining());

        assertEquals(2, backfill.runBatch(2));
        assertEquals(1, backfill.runBatch(2));
        assertEquals(0, backfill.runBatch(2), "running it again is harmless");
        assertEquals(0, backfill.remaining());
        assertEquals("Customer 5", order(old.get(4)).getCustomer());
        assertEquals("New Name", order(both).getCustomer(), "a value written by the new code is never overwritten");
    }

    @Test
    void reads_prefer_the_new_column_and_fall_back_to_the_old_one() throws Exception {
        long notYet = orders.save(new ShopOrder("Grace Hopper", null, "BOOK", 1, 100, OrderStatus.CONFIRMED)).getId();
        long both = orders.save(new ShopOrder("Old Name", "New Name", "BOOK", 1, 100, OrderStatus.CONFIRMED)).getId();
        mvc.perform(get("/orders/" + notYet)).andExpect(status().isOk())
                .andExpect(jsonPath("$.customer").value("Grace Hopper"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(get("/orders/" + both)).andExpect(jsonPath("$.customer").value("New Name"));
        mvc.perform(get("/orders/999999")).andExpect(status().isNotFound());
    }

    // ---- Feature flags ------------------------------------------------------------------------------

    static int bucket(String flag, String user) {
        CRC32 crc = new CRC32();
        crc.update((flag + ":" + user).getBytes(StandardCharsets.UTF_8));
        return (int) (crc.getValue() % 100);
    }

    @Test
    void a_percentage_rollout_is_deterministic_per_user_and_grows_without_flapping() {
        flags.set("new-search", 25, Set.of());
        int on = 0;
        for (int i = 0; i < 1000; i++) {
            String user = "user-" + i;
            boolean enabled = rollout.enabled("new-search", user);
            assertEquals(bucket("new-search", user) < 25, enabled, user);
            assertEquals(enabled, rollout.enabled("new-search", user), "the same user gets the same answer");
            if (enabled) {
                on++;
            }
        }
        assertTrue(on > 200 && on < 300, "about a quarter of the users: " + on);

        flags.set("new-search", 50, Set.of());
        for (int i = 0; i < 1000; i++) {
            String user = "user-" + i;
            if (bucket("new-search", user) < 25) {
                assertTrue(rollout.enabled("new-search", user), "raising the percentage keeps everyone who had it");
            }
        }
    }

    @Test
    void the_allow_list_wins_the_kill_switch_turns_it_off_and_an_unknown_flag_is_off() {
        flags.set("beta", 0, Set.of("ada"));
        assertTrue(rollout.enabled("beta", "ada"));
        assertFalse(rollout.enabled("beta", "bob"));
        flags.set("beta", 100, Set.of());
        assertTrue(rollout.enabled("beta", "bob"));
        flags.set("beta", 0, Set.of());
        for (int i = 0; i < 200; i++) {
            assertFalse(rollout.enabled("beta", "user-" + i), "0% with no allow list is off for everyone");
        }
        assertFalse(rollout.enabled("no-such-flag", "ada"));
    }

    // ---- The endpoint -------------------------------------------------------------------------------

    @Test
    void the_endpoint_routes_by_flag_and_reports_a_cancelled_checkout_as_422() throws Exception {
        flags.set(FlagStore.SAGA_CHECKOUT, 0, Set.of("ada"));
        String lamps = """
                {"customerName": "Ada Lovelace", "sku": "LAMP", "quantity": 5, "totalCents": 9000, "address": "1 Main St"}""";
        String book = """
                {"customerName": "Bob", "sku": "BOOK", "quantity": 1, "totalCents": 2500, "address": "2 Side St"}""";

        mvc.perform(post("/checkout").header("X-User", "ada").contentType(MediaType.APPLICATION_JSON).content(lamps))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.reason").value("out of stock"))
                .andExpect(jsonPath("$.path").value("saga"));
        mvc.perform(post("/checkout").header("X-User", "bob").contentType(MediaType.APPLICATION_JSON).content(book))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value("legacy"));
        assertEquals(1, sagas.count(), "only the user in the rollout went through the saga");
        mvc.perform(post("/checkout").header("X-User", "bob").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\": \"Bob\", \"sku\": \"BOOK\", \"quantity\": 0, \"totalCents\": 2500, \"address\": \"x\"}"))
                .andExpect(status().isBadRequest());
    }
}
