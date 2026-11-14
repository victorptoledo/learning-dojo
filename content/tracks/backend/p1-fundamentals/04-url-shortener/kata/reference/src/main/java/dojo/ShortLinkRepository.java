package dojo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {
    Optional<ShortLink> findByCode(String code);

    boolean existsByCode(String code);

    @Modifying
    @Query("update ShortLink l set l.clicks = l.clicks + 1 where l.code = :code")
    int addClick(@Param("code") String code);

    @Query("select l from ShortLink l order by l.clicks desc, l.code")
    List<ShortLink> mostClicked(Pageable page);
}
