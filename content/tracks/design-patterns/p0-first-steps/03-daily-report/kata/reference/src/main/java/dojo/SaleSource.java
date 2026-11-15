package dojo;

import java.time.LocalDate;
import java.util.List;

/** Where sales come from. The report depends on this, not on the database. */
@FunctionalInterface
public interface SaleSource {
    List<Sale> salesOn(LocalDate day);
}
