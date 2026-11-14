package dojo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<ShopOrder, Long> {
    @Query("select o from ShopOrder o left join fetch o.lines where o.id = :id")
    Optional<ShopOrder> findWithLines(@Param("id") long id);
}
