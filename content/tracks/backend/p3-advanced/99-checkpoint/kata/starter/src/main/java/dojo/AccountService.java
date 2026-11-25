package dojo;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    private final AccountRepository accounts;
    private final EntrySearch search;
    private final ResilientRates rates;

    public AccountService(AccountRepository accounts, EntrySearch search, ResilientRates rates) {
        this.accounts = accounts;
        this.search = search;
        this.rates = rates;
    }

    public AccountView get(String user, long id) {
        throw new UnsupportedOperationException("todo get account " + id + " for " + user);
    }

    public AccountView rename(String user, long id, AccountUpdate update) {
        throw new UnsupportedOperationException("todo rename account " + id + " for " + user);
    }

    public List<EntryView> entries(String user, long id, String q, String sort, int size) {
        throw new UnsupportedOperationException("todo entries of account " + id + " for " + user);
    }

    public BalanceView balance(String user, long id, String currency) {
        throw new UnsupportedOperationException("todo balance of account " + id + " in " + currency);
    }
}
