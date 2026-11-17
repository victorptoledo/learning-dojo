package dojo.wallet;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByOwnerOrderByCurrency(String owner);

    boolean existsByOwnerAndCurrency(String owner, String currency);

    /** Reads the account and locks its row until the transaction ends. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findForUpdate(long id);

    /** One statement: the database groups and counts the entries of every account. */
    @Query("""
            select new dojo.wallet.BalanceRow(a.owner, a.balanceCents, count(e))
            from Account a left join Entry e on e.account = a
            where a.currency = :currency and a.closed = false
            group by a.id, a.owner, a.balanceCents
            order by a.balanceCents desc, a.owner""")
    List<BalanceRow> topBalances(String currency, Limit limit);
}
