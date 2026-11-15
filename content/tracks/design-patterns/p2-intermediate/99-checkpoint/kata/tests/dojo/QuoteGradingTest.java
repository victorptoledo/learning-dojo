package dojo;

import static dojo.GradingSupport.code;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuoteGradingTest {
    private static QuoteRequest.Builder desk() {
        return QuoteRequest.builder().customer("cy").product("desk").unitCents(10_000);
    }

    @Test
    void the_builder_has_defaults_and_validates() {
        QuoteRequest request = QuoteRequest.builder().customer("ana").product("pen").build();
        assertEquals(new QuoteRequest("ana", "pen", 1, 0, false, null, false), request);
        assertTrue(QuoteRequest.class.isRecord(), "QuoteRequest should be a record");
        var noCustomer = assertThrows(IllegalArgumentException.class, () -> QuoteRequest.builder().product("pen").build());
        assertEquals("customer is required", noCustomer.getMessage());
        assertThrows(IllegalArgumentException.class, () -> desk().quantity(0).build());
        assertThrows(IllegalArgumentException.class, () -> desk().unitCents(-1).build());
        assertThrows(IllegalArgumentException.class, () -> QuoteRequest.builder().customer("a").build());
    }

    @Test
    void the_standard_steps_price_like_before() {
        var engine = new QuoteEngine(PricingSteps.standard());
        Quote quote = engine.quote(desk().quantity(12).vip(true).coupon("SAVE5").build());
        // 120000 -5% = 114000, -10% = 102600, -500 = 102100, +23% = 125583
        assertEquals(125_583, quote.cents());
        assertEquals(List.of("bulk -5%", "vip -10%", "coupon SAVE5", "tax 23%"), quote.adjustments());
        assertEquals(new Quote("ana", 0, List.of("tax 23%")),
                engine.quote(QuoteRequest.builder().customer("ana").product("free sample").build()));
        Quote tiny = engine.quote(QuoteRequest.builder().customer("b").product("x").unitCents(300).coupon("SAVE5").build());
        assertEquals(0, tiny.cents(), "a coupon never makes the price negative");
    }

    @Test
    void each_step_works_alone() {
        var request = desk().quantity(10).express(true).build();
        var start = new Quote("cy", 100_000, List.of());
        assertEquals(95_000, PricingSteps.bulkDiscount().apply(request, start).cents());
        assertEquals(start, PricingSteps.vipDiscount().apply(request, start), "a step that does not apply returns the quote unchanged");
        assertEquals(101_500, PricingSteps.expressShipping().apply(request, start).cents());
        assertEquals(123_000, PricingSteps.tax().apply(request, start).cents());
    }

    @Test
    void new_steps_plug_in_anywhere_in_the_chain() {
        PricingStep halfPrice = (request, quote) -> quote.adjusted(quote.cents() / 2, "black friday -50%");
        var steps = new ArrayList<>(PricingSteps.standard());
        steps.add(steps.size() - 1, halfPrice);
        Quote quote = new QuoteEngine(steps).quote(desk().quantity(2).build());
        assertEquals(12_300, quote.cents());
        assertEquals(List.of("black friday -50%", "tax 23%"), quote.adjustments());
        assertEquals(20_000, new QuoteEngine(List.of()).quote(desk().quantity(2).build()).cents());
    }

    @Test
    void listeners_hear_every_quote_until_they_unsubscribe() {
        var engine = new QuoteEngine(PricingSteps.standard());
        List<String> heard = new ArrayList<>();
        Subscription subscription = engine.subscribe((request, quote) -> heard.add(request.product() + "=" + quote.cents()));
        engine.quote(desk().build());
        subscription.close();
        engine.quote(desk().build());
        assertEquals(List.of("desk=12300"), heard);
    }

    @Test
    void a_failing_listener_breaks_neither_the_quote_nor_the_others() {
        var engine = new QuoteEngine(PricingSteps.standard());
        List<Long> heard = new ArrayList<>();
        engine.subscribe((request, quote) -> {
            throw new IllegalStateException("mail server down");
        });
        engine.subscribe((request, quote) -> heard.add(quote.cents()));
        Quote quote = engine.quote(desk().build());
        assertEquals(12_300, quote.cents());
        assertEquals(List.of(12_300L), heard);
        assertEquals(1, engine.failedNotifications());
    }

    @Test
    void the_old_service_still_audits_and_mails() {
        var audit = new AuditLog();
        var mail = new SalesTeamMail();
        var service = new QuoteService(audit, mail);
        service.quote("dee", "sofa", 1, 90_000, false, null, false);
        service.quote("eve", "lamp", 1, 1_000, false, "SAVE5", false);
        assertEquals(List.of("dee sofa x1 = 110700", "eve lamp x1 = 615"), audit.entries());
        assertEquals(List.of("Big quote for dee: 110700"), mail.outbox());
    }

    @Test
    void the_engine_knows_no_rule_and_no_listener() {
        String code = code("dojo.QuoteEngine");
        for (String forbidden : List.of("SAVE5", "vip", "express", "coupon", "AuditLog", "SalesTeamMail", "100_000", "23")) {
            assertFalse(code.contains(forbidden), "QuoteEngine should only run steps and notify listeners; found " + forbidden);
        }
        assertTrue(PricingStep.class.isInterface() && QuoteListener.class.isInterface());
        assertTrue(AutoCloseable.class.isAssignableFrom(Subscription.class), "Subscription should be AutoCloseable");
    }
}
