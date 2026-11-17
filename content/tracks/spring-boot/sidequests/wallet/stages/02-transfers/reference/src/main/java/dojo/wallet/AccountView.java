package dojo.wallet;

/** An account as the API shows it. */
public record AccountView(long id, String owner, String currency, long balanceCents, boolean closed) {
    static AccountView of(Account account) {
        return new AccountView(account.getId(), account.getOwner(), account.getCurrency(), account.getBalanceCents(),
                account.isClosed());
    }
}
