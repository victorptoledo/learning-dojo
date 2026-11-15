# Hexagonal Transfer

`dojo.domain.TransferService` moves money between accounts. It reaches straight into a global
map (`InMemoryBank.ACCOUNTS`), parses the strings of a 2009 rate feed, and prints to the
console. Worse, it debits before it has checked everything: a transfer to an unknown account,
or between currencies the feed cannot price, takes the money and loses it.

Refactor it to **ports and adapters** at the scale of one use case:

- the domain (`dojo.domain`) defines the **ports** it needs, as interfaces in its own terms;
- **adapters** (`dojo.adapters`) implement them: an in-memory repository with transactions,
  an **anti-corruption layer** over the feed, and a console log;
- a **unit of work** makes the two writes of a transfer commit together or not at all;
- one **composition root** (`dojo.App`) is the only place that knows both sides.

`dojo.legacy.LegacyFxFeed` is a library: do not change it.

## The target design

| Type | What it does |
| --- | --- |
| `domain.Account` | a **record** `(String id, String currency, long balanceCents)`; `withdraw(cents)` returns a new account or throws `InsufficientFundsException("insufficient funds in " + id)`; `deposit(cents)` returns a new account |
| `domain.Transfer` | record `(String from, String to, long debitedCents, long creditedCents, BigDecimal rate)` |
| port `domain.Accounts` | `Optional<Account> find(String id)`, `void save(Account account)` |
| port `domain.ExchangeRates` | `BigDecimal rate(String from, String to)`; throws `RateUnavailableException` |
| port `domain.Transactions` | `<T> T inTransaction(Supplier<T> work)`: saves inside `work` commit together, or are all undone if it throws |
| port `domain.TransferLog` | `void record(Transfer transfer)` |
| `domain.TransferService` | `TransferService(Accounts, ExchangeRates, Transactions, TransferLog)`; `Transfer transfer(String fromId, String toId, long cents)` |
| `adapters.InMemoryBank implements Accounts, Transactions` | `open(Account)` adds an account (an existing id throws `IllegalArgumentException`); `save` replaces a known account; `inTransaction` restores a snapshot when the work throws, then rethrows |
| `adapters.LegacyFxRates implements ExchangeRates` | `LegacyFxRates(LegacyFxFeed feed)`. Asks for `from + to`; if the answer is `"N/A"`, asks for `to + from` and returns `1 / rate` (`MathContext.DECIMAL64`, then scale 6, `HALF_EVEN`); if both are `"N/A"`, throws `RateUnavailableException` |
| `adapters.ConsoleTransferLog implements TransferLog` | prints the transfer |
| `dojo.App` | `static TransferService transferService(InMemoryBank bank, LegacyFxFeed feed)` wires the real adapters |

`TransferService.transfer` rules:

- `cents <= 0` or `fromId.equals(toId)` throws `IllegalArgumentException`.
- Inside one transaction: load both accounts (unknown → `IllegalArgumentException("unknown
  account: " + id)`), take the rate (`BigDecimal.ONE` for the same currency, **without**
  asking `ExchangeRates`), compute the credit as `rate × cents` rounded `HALF_EVEN`, then save
  the withdrawn and the deposited account.
- Only after the transaction commits, record the transfer in the log and return it.

## What the grading tests check

- Conversions with the feed's rates, inverted pairs, and the money coming back through an
  inverted rate.
- No rate lookup for same-currency transfers.
- Every failure (no rate, no funds, unknown account, bad amount) leaves every balance alone
  and logs nothing.
- If the **second** save fails, the transaction undoes the first one.
- The dependency rule, ArchUnit-style, by reading your sources: no class in `dojo.domain`
  mentions `dojo.adapters`, `dojo.legacy`, `InMemoryBank`, `LegacyFxFeed` or `System.out`;
  only `dojo.adapters`, `dojo.legacy` and `dojo.App` mention the feed. `TransferService`
  holds its four ports and nothing else.

The visible tests describe the target design, so they do not compile until the ports and
adapters exist. Start with the ports, then move code into the adapters.
