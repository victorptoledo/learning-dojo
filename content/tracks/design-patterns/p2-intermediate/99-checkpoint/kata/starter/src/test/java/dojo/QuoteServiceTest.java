package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Characterisation tests: they pin today's behaviour of QuoteService, so they pass before and
 * after the refactoring. Keep them green while you move the code.
 */
class QuoteServiceTest {
    private final AuditLog audit = new AuditLog();
    private final SalesTeamMail mail = new SalesTeamMail();
    private final QuoteService service = new QuoteService(audit, mail);

    @Test
    void a_plain_quote_only_pays_tax() {
        Quote quote = service.quote("ana", "chair", 2, 5000, false, null, false);
        assertEquals(12_300, quote.cents());
        assertEquals(List.of("tax 23%"), quote.adjustments());
        assertEquals(List.of("ana chair x2 = 12300"), audit.entries());
        assertEquals(List.of(), mail.outbox());
    }

    @Test
    void every_rule_applies_in_order() {
        Quote quote = service.quote("bo", "desk", 10, 10_000, true, "SAVE5", true);
        // 100000 -5% = 95000, -10% = 85500, -500 = 85000, +1500 = 86500, +23% = 106395
        assertEquals(106_395, quote.cents());
        assertEquals(List.of("bulk -5%", "vip -10%", "coupon SAVE5", "express +15.00", "tax 23%"), quote.adjustments());
        assertEquals(List.of("Big quote for bo: 106395"), mail.outbox());
    }

    @Test
    void bad_requests_are_refused() {
        assertThrows(IllegalArgumentException.class, () -> service.quote(" ", "desk", 1, 100, false, null, false));
        assertThrows(IllegalArgumentException.class, () -> service.quote("ana", "desk", 0, 100, false, null, false));
    }
}
