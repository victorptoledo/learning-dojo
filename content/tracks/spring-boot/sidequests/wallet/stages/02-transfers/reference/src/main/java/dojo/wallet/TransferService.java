package dojo.wallet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    private final AccountService accounts;
    private final TransferRepository transfers;

    public TransferService(AccountService accounts, TransferRepository transfers) {
        this.accounts = accounts;
        this.transfers = transfers;
    }

    /** All or nothing: any exception rolls back both balances. */
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
        return transfers.save(new Transfer(fromId, toId, request.amountCents()));
    }

    @Transactional(readOnly = true)
    public Transfer get(long id) {
        return transfers.findById(id).orElseThrow(() -> new NotFoundException("transfer", id));
    }
}
