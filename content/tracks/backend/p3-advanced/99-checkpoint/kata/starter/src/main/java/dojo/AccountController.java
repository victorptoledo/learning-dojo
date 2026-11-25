package dojo;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }
}
