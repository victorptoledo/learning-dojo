package dojo.wallet;

import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatementController {
    private final StatementService statements;

    public StatementController(StatementService statements) {
        this.statements = statements;
    }

    @GetMapping("/accounts/{id}/statement")
    public Statement statement(@PathVariable long id, @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return statements.of(id, month);
    }
}
