package dojo;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SagaRepository extends JpaRepository<SagaRecord, String> {
    List<SagaRecord> findByStepIn(Collection<SagaStep> steps);
}
