package dojo.domain;

import dojo.adapters.InMemoryBank;
import dojo.legacy.LegacyFxFeed;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Moves money between accounts. The domain reaches straight into the global account map, the
 * 2009 rate feed and the console, and it debits before it has checked everything: a transfer
 * to an unknown account, or between currencies without a rate, loses the money.
 */
public class TransferService {
    public void transfer(String fromId, String toId, long cents) {
        Account from = InMemoryBank.ACCOUNTS.get(fromId);
        if (from == null) {
            throw new IllegalArgumentException("unknown account: " + fromId);
        }
        if (from.getBalanceCents() < cents) {
            throw new InsufficientFundsException("insufficient funds in " + fromId);
        }
        from.setBalanceCents(from.getBalanceCents() - cents);

        Account to = InMemoryBank.ACCOUNTS.get(toId);
        if (to == null) {
            throw new IllegalArgumentException("unknown account: " + toId);
        }
        long credited = cents;
        if (!from.getCurrency().equals(to.getCurrency())) {
            String answer = new LegacyFxFeed().quote(from.getCurrency() + to.getCurrency());
            if (answer.equals("N/A")) {
                throw new RateUnavailableException(from.getCurrency() + " to " + to.getCurrency());
            }
            BigDecimal rate = new BigDecimal(answer.substring(answer.indexOf('=') + 1));
            credited = rate.multiply(BigDecimal.valueOf(cents)).setScale(0, RoundingMode.HALF_EVEN).longValueExact();
        }
        to.setBalanceCents(to.getBalanceCents() + credited);
        System.out.println("transferred " + cents + " " + from.getCurrency() + " from " + fromId + " to " + toId);
    }
}
