package dojo.wallet;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntryRepository extends JpaRepository<Entry, Long> {
    Page<Entry> findByAccountIdOrderByIdDesc(long accountId, Pageable pageable);

    Page<Entry> findByAccountIdAndTypeOrderByIdDesc(long accountId, EntryType type, Pageable pageable);

    /** The last entry before `at`: its balance is the balance at that moment. */
    Optional<Entry> findFirstByAccountIdAndAtBeforeOrderByAtDescIdDesc(long accountId, Instant at);

    /** Money in, money out and the number of entries between two instants, in one statement. */
    @Query("""
            select new dojo.wallet.MonthTotals(
                coalesce(sum(case when e.type in :incoming then e.amountCents else 0 end), 0),
                coalesce(sum(case when e.type in :incoming then 0 else e.amountCents end), 0),
                count(e))
            from Entry e
            where e.account.id = :accountId and e.at >= :from and e.at < :to""")
    MonthTotals totals(long accountId, Instant from, Instant to, Collection<EntryType> incoming);
}
