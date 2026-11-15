package dojo.adapters;

import dojo.domain.Account;
import java.util.HashMap;
import java.util.Map;

/** The accounts, in a global map that every class reads and writes directly. */
public final class InMemoryBank {
    public static final Map<String, Account> ACCOUNTS = new HashMap<>();

    private InMemoryBank() {}
}
