package dojo.tickets;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** The organizer side of an event; SecurityConfig keeps buyers out. */
@RestController
public class SaleController {
    private final SaleService sales;

    public SaleController(SaleService sales) {
        this.sales = sales;
    }

    @DeleteMapping("/events/{id}")
    public SaleService.Cancellation cancel(@PathVariable long id) {
        return sales.cancel(id);
    }

    @GetMapping("/events/{id}/report")
    public SaleService.Report report(@PathVariable long id) {
        return sales.report(id);
    }
}
