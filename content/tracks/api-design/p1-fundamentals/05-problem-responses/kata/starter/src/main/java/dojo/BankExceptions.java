package dojo;

/** The domain errors of the bank. Written for you: each carries what a client needs to react. */
public final class BankExceptions {
    private BankExceptions() {}

    public static final class AccountNotFoundException extends RuntimeException {
        private final String accountId;

        public AccountNotFoundException(String accountId) {
            super("account " + accountId + " does not exist");
            this.accountId = accountId;
        }

        public String accountId() {
            return accountId;
        }
    }

    public static final class AccountFrozenException extends RuntimeException {
        public AccountFrozenException(String accountId) {
            super("account " + accountId + " is frozen");
        }
    }

    public static final class InsufficientFundsException extends RuntimeException {
        private final long balanceCents;
        private final long requestedCents;

        public InsufficientFundsException(long balanceCents, long requestedCents) {
            super("balance " + balanceCents + " is less than the " + requestedCents + " requested");
            this.balanceCents = balanceCents;
            this.requestedCents = requestedCents;
        }

        public long balanceCents() {
            return balanceCents;
        }

        public long requestedCents() {
            return requestedCents;
        }
    }
}
