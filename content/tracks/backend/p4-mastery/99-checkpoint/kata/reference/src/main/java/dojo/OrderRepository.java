package dojo;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<ShopOrder, Long> {
    @Query("select o.id from ShopOrder o where o.customer is null order by o.id")
    List<Long> idsWithoutCustomer(Pageable page);

    /** Never overwrites a customer that is already there. */
    @Modifying
    @Query("update ShopOrder o set o.customer = o.customerName where o.id in :ids and o.customer is null")
    int copyCustomerName(@Param("ids") List<Long> ids);

    long countByCustomerIsNull();
}
