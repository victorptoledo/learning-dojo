package dojo;

import dojo.BankExceptions.AccountFrozenException;
import dojo.BankExceptions.AccountNotFoundException;
import dojo.BankExceptions.InsufficientFundsException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * The bank, in memory. Written for you. Seeded with acc-1 (Ada, 100.00), acc-2 (Linus, 5.00)
 * and acc-3 (Grace, 20.00, frozen).
 */
@Service
public class Bank {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final AtomicLong nextWithdrawal = new AtomicLong(1);

    public Bank() {
        accounts.put("acc-1", new Account("acc-1", "Ada", 10_000, false));
        accounts.put("acc-2", new Account("acc-2", "Linus", 500, false));
        accounts.put("acc-3", new Account("acc-3", "Grace", 2_000, true));
    }

    public Account account(String id) {
        Account account = accounts.get(id);
        if (account == null) {
            throw new AccountNotFoundException(id);
        }
        return account;
    }

    public synchronized Withdrawal withdraw(String accountId, WithdrawalRequest request) {
        Account account = account(accountId);
        long amount = request.amountCents();
        if (amount == 666) {
            // A bug deep in a dependency: its message is not for clients.
            throw new IllegalStateException("LedgerClient NPE at 10.0.3.7:5432 (user=ledger_rw)");
        }
        if (account.frozen()) {
            throw new AccountFrozenException(accountId);
        }
        if (account.balanceCents() < amount) {
            throw new InsufficientFundsException(account.balanceCents(), amount);
        }
        Account after = account.withBalance(account.balanceCents() - amount);
        accounts.put(accountId, after);
        return new Withdrawal(nextWithdrawal.getAndIncrement(), accountId, amount, request.reference(),
                after.balanceCents());
    }
}
