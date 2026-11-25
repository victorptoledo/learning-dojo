package dojo;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts/{id}")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public AccountView get(@RequestHeader("X-User") String user, @PathVariable long id) {
        return accounts.get(user, id);
    }

    @PatchMapping
    public AccountView rename(@RequestHeader("X-User") String user, @PathVariable long id,
                              @Valid @RequestBody AccountUpdate update) {
        return accounts.rename(user, id, update);
    }

    @GetMapping("/entries")
    public List<EntryView> entries(@RequestHeader("X-User") String user, @PathVariable long id,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(defaultValue = "-bookedOn") String sort,
                                   @RequestParam(defaultValue = "20") int size) {
        return accounts.entries(user, id, q, sort, size);
    }

    @GetMapping("/balance")
    public BalanceView balance(@RequestHeader("X-User") String user, @PathVariable long id,
                               @RequestParam("in") String currency) {
        return accounts.balance(user, id, currency);
    }
}
