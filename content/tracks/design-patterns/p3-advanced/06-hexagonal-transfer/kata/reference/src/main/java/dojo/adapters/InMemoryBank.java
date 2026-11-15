package dojo.adapters;

import dojo.domain.Account;
import dojo.domain.Accounts;
import dojo.domain.Transactions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Driven adapter: accounts in memory, with transactions that roll back by restoring a snapshot. */
public final class InMemoryBank implements Accounts, Transactions {
    private final Map<String, Account> accounts = new LinkedHashMap<>();

    public void open(Account account) {
        if (accounts.containsKey(account.id())) {
            throw new IllegalArgumentException("account exists: " + account.id());
        }
        accounts.put(account.id(), account);
    }

    @Override
    public Optional<Account> find(String id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public void save(Account account) {
        if (!accounts.containsKey(account.id())) {
            throw new IllegalArgumentException("unknown account: " + account.id());
        }
        accounts.put(account.id(), account);
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        Map<String, Account> snapshot = new LinkedHashMap<>(accounts);
        try {
            return work.get();
        } catch (RuntimeException e) {
            accounts.clear();
            accounts.putAll(snapshot);
            throw e;
        }
    }
}
