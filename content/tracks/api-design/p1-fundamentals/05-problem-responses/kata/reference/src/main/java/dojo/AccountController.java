package dojo;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** GET /accounts/{id} and POST /accounts/{id}/withdrawals. Errors are ApiProblems' job. */
@RestController
public class AccountController {
    private final Bank bank;

    public AccountController(Bank bank) {
        this.bank = bank;
    }

    @GetMapping("/accounts/{id}")
    public Account account(@PathVariable String id) {
        return bank.account(id);
    }

    @PostMapping("/accounts/{id}/withdrawals")
    public ResponseEntity<Withdrawal> withdraw(@PathVariable String id, @Valid @RequestBody WithdrawalRequest request) {
        Withdrawal withdrawal = bank.withdraw(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{withdrawal}")
                .buildAndExpand(withdrawal.id())
                .toUri();
        return ResponseEntity.created(location).body(withdrawal);
    }
}
