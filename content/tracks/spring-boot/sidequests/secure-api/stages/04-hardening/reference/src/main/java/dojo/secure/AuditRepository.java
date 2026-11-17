package dojo.secure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findTop100ByUsernameOrderByIdDesc(String username);

    List<AuditEvent> findTop100ByOrderByIdDesc();
}
