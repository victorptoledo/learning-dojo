package dojo.wallet;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LedgerController {
    private final Ledger ledger;
    private final AccountRepository accounts;

    public LedgerController(Ledger ledger, AccountRepository accounts) {
        this.ledger = ledger;
        this.accounts = accounts;
    }

    @GetMapping("/accounts/{id}/entries")
    public PageView<EntryView> entries(@PathVariable long id,
            @RequestParam(required = false) EntryType type,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageView.of(ledger.page(id, type, page, size), EntryView::of);
    }

    @GetMapping("/reports/balances")
    public List<BalanceRow> balances(@RequestParam String currency,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return accounts.topBalances(currency, Limit.of(limit));
    }
}
