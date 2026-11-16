package dojo;

/** A bank account. Balances are in cents. */
public record Account(String id, String owner, long balanceCents, boolean frozen) {

    Account withBalance(long newBalanceCents) {
        return new Account(id, owner, newBalanceCents, frozen);
    }
}
