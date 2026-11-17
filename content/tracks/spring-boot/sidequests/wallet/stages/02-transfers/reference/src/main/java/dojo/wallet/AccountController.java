package dojo.wallet;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    public ResponseEntity<AccountView> open(@Valid @RequestBody NewAccount request, UriComponentsBuilder uri) {
        Account account = accounts.open(request);
        return ResponseEntity.created(uri.path("/accounts/{id}").build(account.getId())).body(AccountView.of(account));
    }

    @GetMapping("/{id}")
    public AccountView get(@PathVariable long id) {
        return AccountView.of(accounts.get(id));
    }

    @GetMapping
    public List<AccountView> ofOwner(@RequestParam String owner) {
        return accounts.ofOwner(owner).stream().map(AccountView::of).toList();
    }

    @PostMapping("/{id}/deposits")
    public AccountView deposit(@PathVariable long id, @Valid @RequestBody Amount amount) {
        return AccountView.of(accounts.deposit(id, amount.amountCents()));
    }

    @PostMapping("/{id}/withdrawals")
    public AccountView withdraw(@PathVariable long id, @Valid @RequestBody Amount amount) {
        return AccountView.of(accounts.withdraw(id, amount.amountCents()));
    }

    @PostMapping("/{id}/close")
    public AccountView close(@PathVariable long id) {
        return AccountView.of(accounts.close(id));
    }
}
