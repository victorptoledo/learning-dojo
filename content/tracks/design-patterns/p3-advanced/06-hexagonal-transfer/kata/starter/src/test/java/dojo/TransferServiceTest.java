package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dojo.adapters.InMemoryBank;
import dojo.adapters.LegacyFxRates;
import dojo.domain.Account;
import dojo.domain.Transfer;
import dojo.domain.TransferService;
import dojo.legacy.LegacyFxFeed;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * These tests describe the design you are refactoring towards, so they do not compile until
 * the ports and adapters exist (see README.md).
 */
class TransferServiceTest {
    @Test
    void money_moves_between_accounts_in_the_same_currency() {
        var bank = new InMemoryBank();
        bank.open(new Account("a", "EUR", 10_000));
        bank.open(new Account("b", "EUR", 0));
        List<Transfer> log = new ArrayList<>();
        var service = new TransferService(bank, (from, to) -> BigDecimal.ONE, bank, log::add);

        service.transfer("a", "b", 2_500);

        assertEquals(7_500, bank.find("a").orElseThrow().balanceCents());
        assertEquals(2_500, bank.find("b").orElseThrow().balanceCents());
        assertEquals(1, log.size());
    }

    @Test
    void the_legacy_feed_hides_behind_an_anti_corruption_layer() {
        var rates = new LegacyFxRates(new LegacyFxFeed());
        assertEquals(new BigDecimal("1.0842"), rates.rate("EUR", "USD"));
    }

    @Test
    void a_failed_transfer_changes_nothing() {
        var bank = new InMemoryBank();
        bank.open(new Account("a", "EUR", 10_000));
        var service = new TransferService(bank, (from, to) -> BigDecimal.ONE, bank, transfer -> {});
        assertThrows(IllegalArgumentException.class, () -> service.transfer("a", "nobody", 100));
        assertEquals(10_000, bank.find("a").orElseThrow().balanceCents());
    }
}
