package dojo.domain;

import java.util.Optional;

/** Driven port: where accounts live (the repository). */
public interface Accounts {
    Optional<Account> find(String id);

    void save(Account account);
}
