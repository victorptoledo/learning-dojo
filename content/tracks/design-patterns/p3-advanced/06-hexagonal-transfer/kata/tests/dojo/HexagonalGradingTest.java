package dojo;

import static dojo.GradingSupport.allClasses;
import static dojo.GradingSupport.code;
import static dojo.GradingSupport.instanceFieldTypes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.adapters.InMemoryBank;
import dojo.adapters.LegacyFxRates;
import dojo.domain.Account;
import dojo.domain.Accounts;
import dojo.domain.ExchangeRates;
import dojo.domain.InsufficientFundsException;
import dojo.domain.RateUnavailableException;
import dojo.domain.Transactions;
import dojo.domain.Transfer;
import dojo.domain.TransferLog;
import dojo.domain.TransferService;
import dojo.legacy.LegacyFxFeed;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HexagonalGradingTest {
    private final InMemoryBank bank = new InMemoryBank();
    private final List<Transfer> log = new ArrayList<>();

    private TransferService service() {
        return new TransferService(bank, new LegacyFxRates(new LegacyFxFeed()), bank, log::add);
    }

    private long balance(String id) {
        return bank.find(id).orElseThrow().balanceCents();
    }

    private void open(String id, String currency, long cents) {
        bank.open(new Account(id, currency, cents));
    }

    @Test
    void transfers_convert_with_the_rate_of_the_feed() {
        open("eur", "EUR", 50_000);
        open("usd", "USD", 0);
        Transfer transfer = service().transfer("eur", "usd", 10_000);
        assertEquals(new Transfer("eur", "usd", 10_000, 10_842, new BigDecimal("1.0842")), transfer);
        assertEquals(40_000, balance("eur"));
        assertEquals(10_842, balance("usd"));
        assertEquals(List.of(transfer), log);
    }

    @Test
    void the_anti_corruption_layer_inverts_one_way_pairs_and_translates_errors() {
        var rates = new LegacyFxRates(new LegacyFxFeed());
        assertEquals(new BigDecimal("1.1650"), rates.rate("GBP", "EUR"));
        assertEquals(new BigDecimal("0.922339"), rates.rate("USD", "EUR"));
        assertEquals(new BigDecimal("0.858369"), rates.rate("EUR", "GBP"));
        assertThrows(RateUnavailableException.class, () -> rates.rate("EUR", "JPY"));
    }

    @Test
    void the_inverse_rate_brings_the_money_back() {
        open("usd", "USD", 10_842);
        open("eur", "EUR", 0);
        service().transfer("usd", "eur", 10_842);
        assertEquals(10_000, balance("eur"));
    }

    @Test
    void same_currency_transfers_never_ask_for_a_rate() {
        open("a", "EUR", 1_000);
        open("b", "EUR", 0);
        ExchangeRates noRates = (from, to) -> {
            throw new AssertionError("no rate needed for EUR to EUR");
        };
        new TransferService(bank, noRates, bank, log::add).transfer("a", "b", 999);
        assertEquals(1, balance("a"));
        assertEquals(999, balance("b"));
    }

    @Test
    void a_failed_transfer_leaves_every_balance_alone() {
        open("eur", "EUR", 1_000);
        open("jpy", "JPY", 0);
        open("eur2", "EUR", 0);
        assertThrows(RateUnavailableException.class, () -> service().transfer("eur", "jpy", 100));
        assertThrows(InsufficientFundsException.class, () -> service().transfer("eur", "eur2", 5_000));
        var unknown = assertThrows(IllegalArgumentException.class, () -> service().transfer("eur", "ghost", 100));
        assertEquals("unknown account: ghost", unknown.getMessage());
        assertThrows(IllegalArgumentException.class, () -> service().transfer("eur", "eur", 100));
        assertThrows(IllegalArgumentException.class, () -> service().transfer("eur", "jpy", 0));
        assertEquals(1_000, balance("eur"));
        assertEquals(0, balance("jpy"));
        assertEquals(0, balance("eur2"));
        assertEquals(List.of(), log, "only committed transfers are logged");
    }

    @Test
    void the_unit_of_work_rolls_back_a_half_written_transfer() {
        open("a", "EUR", 1_000);
        open("b", "EUR", 0);
        int[] saves = {0};
        Accounts failsOnSecondSave = new Accounts() {
            @Override
            public Optional<Account> find(String id) {
                return bank.find(id);
            }

            @Override
            public void save(Account account) {
                if (++saves[0] == 2) throw new IllegalStateException("disk full");
                bank.save(account);
            }
        };
        var service = new TransferService(failsOnSecondSave, (from, to) -> BigDecimal.ONE, bank, log::add);
        assertThrows(IllegalStateException.class, () -> service.transfer("a", "b", 400));
        assertEquals(1_000, balance("a"), "the first save must be rolled back by the transaction");
        assertEquals(0, balance("b"));
        assertEquals(List.of(), log);
    }

    @Test
    void the_composition_root_wires_the_real_adapters() {
        open("eur", "EUR", 100);
        open("usd", "USD", 0);
        Transfer transfer = App.transferService(bank, new LegacyFxFeed()).transfer("eur", "usd", 100);
        assertEquals(108, transfer.creditedCents());
        assertEquals(108, balance("usd"));
    }

    @Test
    void accounts_are_values() {
        assertTrue(Account.class.isRecord(), "Account should be a record");
        var account = new Account("x", "EUR", 100);
        assertEquals(new Account("x", "EUR", 40), account.withdraw(60));
        assertEquals(new Account("x", "EUR", 100), account);
        assertThrows(InsufficientFundsException.class, () -> account.withdraw(101));
    }

    @Test
    void the_service_depends_on_ports_only() {
        for (Class<?> port : List.of(Accounts.class, ExchangeRates.class, Transactions.class, TransferLog.class)) {
            assertTrue(port.isInterface(), port.getSimpleName() + " must be an interface (a port)");
        }
        assertEquals(List.of(Accounts.class, ExchangeRates.class, Transactions.class, TransferLog.class),
                instanceFieldTypes(TransferService.class).stream().sorted((x, y) -> order(x) - order(y)).toList(),
                "TransferService should hold its four ports and nothing else");
        assertTrue(Accounts.class.isAssignableFrom(InMemoryBank.class) && Transactions.class.isAssignableFrom(InMemoryBank.class));
        assertTrue(ExchangeRates.class.isAssignableFrom(LegacyFxRates.class));
    }

    @Test
    void the_domain_knows_no_adapter_and_no_legacy_code() {
        List<String> domain = allClasses().stream().filter(name -> name.startsWith("dojo.domain.")).toList();
        assertTrue(domain.size() >= 8, "expected the domain classes and ports in dojo.domain");
        for (String name : domain) {
            String code = code(name);
            assertFalse(code.contains("dojo.adapters"), name + " must not depend on the adapters");
            assertFalse(code.contains("dojo.legacy"), name + " must not depend on the legacy feed");
            assertFalse(code.contains("InMemoryBank") || code.contains("LegacyFxFeed"), name + " names an adapter");
            assertFalse(code.contains("System.out"), name + " must not print: that is an adapter's job");
        }
        for (String name : allClasses()) {
            boolean allowed = name.startsWith("dojo.adapters.") || name.startsWith("dojo.legacy.") || name.equals("dojo.App");
            if (!allowed) {
                assertFalse(code(name).contains("LegacyFxFeed"), name + ": only the adapters and App may know the feed");
            }
        }
    }

    private static int order(Class<?> port) {
        return List.of(Accounts.class, ExchangeRates.class, Transactions.class, TransferLog.class).indexOf(port);
    }
}
