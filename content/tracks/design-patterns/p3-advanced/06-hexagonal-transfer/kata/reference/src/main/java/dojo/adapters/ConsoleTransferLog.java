package dojo.adapters;

import dojo.domain.Transfer;
import dojo.domain.TransferLog;

/** Driven adapter: prints transfers, as the old code did. */
public final class ConsoleTransferLog implements TransferLog {
    @Override
    public void record(Transfer transfer) {
        System.out.println("transferred " + transfer.debitedCents() + " from " + transfer.from() + " to " + transfer.to());
    }
}
