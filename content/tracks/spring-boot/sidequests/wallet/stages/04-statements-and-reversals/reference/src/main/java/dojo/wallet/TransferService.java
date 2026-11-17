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
        if (request.from().equals(request.to())) {
            throw new FieldException("to", "must differ from from");
        }
        return move(request.from(), request.to(), request.amountCents(), null);
    }

    /** A new transfer the other way; the lock on the original makes the check safe. */
    @Transactional
    public Transfer reverse(long id) {
        Transfer original = transfers.findForUpdate(id).orElseThrow(() -> new NotFoundException("transfer", id));
        if (original.getReverses() != null) {
            throw new RuleException("a reversal cannot be reversed");
        }
        if (transfers.existsByReverses(id)) {
            throw new ConflictException("transfer " + id + " is already reversed");
        }
        return move(original.getTo(), original.getFrom(), original.getAmountCents(), id);
    }

    private Transfer move(long fromId, long toId, long cents, Long reverses) {
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
        from.debit(cents);
        to.credit(cents);
        Transfer transfer = transfers.save(new Transfer(fromId, toId, cents, reverses));
        ledger.record(from, EntryType.TRANSFER_OUT, cents, transfer.getId());
        ledger.record(to, EntryType.TRANSFER_IN, cents, transfer.getId());
        return transfer;
    }

    @Transactional(readOnly = true)
    public Transfer get(long id) {
        return transfers.findById(id).orElseThrow(() -> new NotFoundException("transfer", id));
    }
}
