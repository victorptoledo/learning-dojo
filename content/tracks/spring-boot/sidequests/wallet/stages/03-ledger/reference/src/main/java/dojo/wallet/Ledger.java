package dojo.wallet;

import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes and reads the entries of the accounts. */
@Service
public class Ledger {
    private final EntryRepository entries;
    private final AccountService accounts;
    private final Clock clock;

    public Ledger(EntryRepository entries, AccountService accounts, Clock clock) {
        this.entries = entries;
        this.accounts = accounts;
        this.clock = clock;
    }

    /** Records a change already applied to `account`; must run inside its transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Account account, EntryType type, long cents, Long transferId) {
        entries.save(new Entry(account, type, cents, transferId, Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public Page<Entry> page(long accountId, EntryType type, int page, int size) {
        accounts.get(accountId);
        PageRequest request = PageRequest.of(page, size);
        return type == null
                ? entries.findByAccountIdOrderByIdDesc(accountId, request)
                : entries.findByAccountIdAndTypeOrderByIdDesc(accountId, type, request);
    }
}
