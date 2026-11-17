package dojo.wallet;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
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
}
