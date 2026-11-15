package dojo.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** The application service: the use case "transfer money", in terms of its ports only. */
public class TransferService {
    private final Accounts accounts;
    private final ExchangeRates rates;
    private final Transactions transactions;
    private final TransferLog log;

    public TransferService(Accounts accounts, ExchangeRates rates, Transactions transactions, TransferLog log) {
        this.accounts = accounts;
        this.rates = rates;
        this.transactions = transactions;
        this.log = log;
    }

    public Transfer transfer(String fromId, String toId, long cents) {
        if (cents <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("cannot transfer to the same account");
        }
        Transfer transfer = transactions.inTransaction(() -> {
            Account from = load(fromId);
            Account to = load(toId);
            BigDecimal rate = from.currency().equals(to.currency())
                    ? BigDecimal.ONE
                    : rates.rate(from.currency(), to.currency());
            long credited = rate.multiply(BigDecimal.valueOf(cents)).setScale(0, RoundingMode.HALF_EVEN).longValueExact();
            accounts.save(from.withdraw(cents));
            accounts.save(to.deposit(credited));
            return new Transfer(fromId, toId, cents, credited, rate);
        });
        log.record(transfer);
        return transfer;
    }

    private Account load(String id) {
        return accounts.find(id).orElseThrow(() -> new IllegalArgumentException("unknown account: " + id));
    }
}
