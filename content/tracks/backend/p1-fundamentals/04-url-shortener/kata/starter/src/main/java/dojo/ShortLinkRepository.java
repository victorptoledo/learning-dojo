package dojo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {
    // find by code, does a code exist, add one click atomically, the most clicked links
}
