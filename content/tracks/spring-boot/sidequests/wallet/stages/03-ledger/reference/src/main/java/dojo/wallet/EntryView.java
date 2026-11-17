package dojo.wallet;

import java.time.Instant;

/** An entry as the API shows it. */
public record EntryView(long id, EntryType type, long amountCents, long balanceAfterCents, Long transferId, Instant at) {
    static EntryView of(Entry entry) {
        return new EntryView(entry.getId(), entry.getType(), entry.getAmountCents(), entry.getBalanceAfterCents(),
                entry.getTransferId(), entry.getAt());
    }
}
