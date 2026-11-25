package dojo;

/** What a client sees of an account: not the owner, not the overdraft limit. */
public record AccountView(long id, String nickname, String currency, long balanceCents) {
    public static AccountView of(Account account) {
        return new AccountView(account.getId(), account.getNickname(), account.getCurrency(), account.getBalanceCents());
    }
}
