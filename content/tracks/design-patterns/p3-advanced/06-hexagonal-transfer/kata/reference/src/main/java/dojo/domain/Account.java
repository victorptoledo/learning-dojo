package dojo.domain;

/** A bank account as a value: changes give a new account. */
public record Account(String id, String currency, long balanceCents) {
    public Account withdraw(long cents) {
        if (cents > balanceCents) {
            throw new InsufficientFundsException("insufficient funds in " + id);
        }
        return new Account(id, currency, balanceCents - cents);
    }

    public Account deposit(long cents) {
        return new Account(id, currency, balanceCents + cents);
    }
}
