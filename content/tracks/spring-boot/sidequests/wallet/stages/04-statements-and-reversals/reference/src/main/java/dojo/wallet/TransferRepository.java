package dojo.wallet;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    /** Reads the transfer and locks its row until the transaction ends. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transfer t where t.id = :id")
    Optional<Transfer> findForUpdate(long id);

    boolean existsByReverses(long id);
}
