package dojo.wallet;

/** A transfer as the API shows it. */
public record TransferView(long id, long from, long to, long amountCents) {
    static TransferView of(Transfer transfer) {
        return new TransferView(transfer.getId(), transfer.getFrom(), transfer.getTo(), transfer.getAmountCents());
    }
}
