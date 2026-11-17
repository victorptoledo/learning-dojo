package dojo.wallet;

import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final AccountRepository accounts;
    private final Ledger ledger;

    public AccountService(AccountRepository accounts, @Lazy Ledger ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    /** Opens an account; the unique constraint catches the duplicates the check misses. */
    public Account open(NewAccount request) {
        String owner = request.owner().strip();
        if (accounts.existsByOwnerAndCurrency(owner, request.currency())) {
            throw new ConflictException(owner + " already has a " + request.currency() + " account");
        }
        return accounts.saveAndFlush(new Account(owner, request.currency()));
    }

    @Transactional(readOnly = true)
    public Account get(long id) {
        return accounts.findById(id).orElseThrow(() -> new NotFoundException("account", id));
    }

    /** The account, locked until the current transaction ends. */
    Account lock(long id) {
        return accounts.findForUpdate(id).orElseThrow(() -> new NotFoundException("account", id));
    }

    @Transactional(readOnly = true)
    public List<Account> ofOwner(String owner) {
        return accounts.findByOwnerOrderByCurrency(owner);
    }

    @Transactional
    public Account deposit(long id, long cents) {
        Account account = lock(id);
        account.credit(cents);
        ledger.record(account, EntryType.DEPOSIT, cents, null);
        return account;
    }

    @Transactional
    public Account withdraw(long id, long cents) {
        Account account = lock(id);
        account.debit(cents);
        ledger.record(account, EntryType.WITHDRAWAL, cents, null);
        return account;
    }

    @Transactional
    public Account close(long id) {
        Account account = lock(id);
        account.close();
        return account;
    }
}
