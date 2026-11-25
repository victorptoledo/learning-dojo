package dojo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** The account if it exists and belongs to the user; the same 404 otherwise (BOLA). */
    private Account owned(String user, long id) {
        return accounts.findById(id)
                .filter(account -> account.getOwner().equals(user))
                .orElseThrow(StatementsException::accountNotFound);
    }

    public AccountView get(String user, long id) {
        return AccountView.of(owned(user, id));
    }

    @Transactional
    public AccountView rename(String user, long id, AccountUpdate update) {
        Account account = owned(user, id);
        account.rename(update.nickname());
        return AccountView.of(account);
    }

    public List<EntryView> entries(String user, long id, String q, String sort, int size) {
        owned(user, id);
        return search.find(id, q, sort, size);
    }

    public BalanceView balance(String user, long id, String currency) {
        Account account = owned(user, id);
        RateQuote quote = rates.rate(account.getCurrency(), currency);
        long amount = BigDecimal.valueOf(account.getBalanceCents()).multiply(quote.rate())
                .setScale(0, RoundingMode.HALF_EVEN).longValueExact();
        return new BalanceView(currency, amount, quote.stale());
    }
}
