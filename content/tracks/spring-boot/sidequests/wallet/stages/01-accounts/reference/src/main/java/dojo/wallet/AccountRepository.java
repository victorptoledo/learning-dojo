package dojo.wallet;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByOwnerOrderByCurrency(String owner);

    boolean existsByOwnerAndCurrency(String owner, String currency);
}
