package dojo.wallet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    private final AccountService accounts;
    private final TransferRepository transfers;
    private final Ledger ledger;

    public TransferService(AccountService accounts, TransferRepository transfers, Ledger ledger) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.ledger = ledger;
    }

    /** All or nothing: any exception rolls back both balances and their entries. */
    @Transactional
    public Transfer transfer(NewTransfer request) {
        long fromId = request.from();
        long toId = request.to();
        if (fromId == toId) {
            throw new FieldException("to", "must differ from from");
        }
        // Lock both rows in id order, so opposite transfers never wait for each other.
        Account first = accounts.lock(Math.min(fromId, toId));
        Account second = accounts.lock(Math.max(fromId, toId));
        Account from = first.getId() == fromId ? first : second;
        Account to = from == first ? second : first;

        from.ensureOpen();
        to.ensureOpen();
        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new RuleException("cannot move " + from.getCurrency() + " to " + to.getCurrency());
        }
        from.debit(request.amountCents());
        to.credit(request.amountCents());
        Transfer transfer = transfers.save(new Transfer(fromId, toId, request.amountCents()));
        ledger.record(from, EntryType.TRANSFER_OUT, request.amountCents(), transfer.getId());
        ledger.record(to, EntryType.TRANSFER_IN, request.amountCents(), transfer.getId());
        return transfer;
    }

    @Transactional(readOnly = true)
    public Transfer get(long id) {
        return transfers.findById(id).orElseThrow(() -> new NotFoundException("transfer", id));
    }
}
