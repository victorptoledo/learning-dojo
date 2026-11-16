package dojo;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /accounts/{id} and POST /accounts/{id}/withdrawals. Every failure is a 200 with an
 * "error" string today, and nothing is validated.
 */
@RestController
public class AccountController {
    private final Bank bank;

    public AccountController(Bank bank) {
        this.bank = bank;
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<?> account(@PathVariable String id) {
        try {
            return ResponseEntity.ok(bank.account(id));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/accounts/{id}/withdrawals")
    public ResponseEntity<?> withdraw(@PathVariable String id, @RequestBody WithdrawalRequest request) {
        try {
            return ResponseEntity.ok(bank.withdraw(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }
}
