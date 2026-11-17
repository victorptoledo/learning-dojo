package dojo.wallet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryRepository extends JpaRepository<Entry, Long> {
    Page<Entry> findByAccountIdOrderByIdDesc(long accountId, Pageable pageable);

    Page<Entry> findByAccountIdAndTypeOrderByIdDesc(long accountId, EntryType type, Pageable pageable);
}
