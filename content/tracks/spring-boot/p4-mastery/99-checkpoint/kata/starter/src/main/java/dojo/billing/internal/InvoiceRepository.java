package dojo.billing.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByNumber(String number);
}
