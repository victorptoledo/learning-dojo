package dojo.wallet;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Statements computed by the database: four small queries, whatever the number of entries. */
@Service
public class StatementService {
    private static final List<EntryType> INCOMING = List.of(EntryType.DEPOSIT, EntryType.TRANSFER_IN);

    private final AccountService accounts;
    private final EntryRepository entries;

    public StatementService(AccountService accounts, EntryRepository entries) {
        this.accounts = accounts;
        this.entries = entries;
    }

    @Transactional(readOnly = true)
    public Statement of(long accountId, YearMonth month) {
        accounts.get(accountId);
        Instant from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        long opening = balanceBefore(accountId, from, 0);
        long closing = balanceBefore(accountId, to, opening);
        MonthTotals totals = entries.totals(accountId, from, to, INCOMING);
        return new Statement(month, opening, closing, totals.inCents(), totals.outCents(), totals.entries());
    }

    private long balanceBefore(long accountId, Instant at, long otherwise) {
        return entries.findFirstByAccountIdAndAtBeforeOrderByAtDescIdDesc(accountId, at)
                .map(Entry::getBalanceAfterCents)
                .orElse(otherwise);
    }
}
