# Car Rental

`dojo.rentals` has two contexts. **Booking** decides whether a customer may rent a car and at
what price; **billing** charges them. Customers live in a legacy CRM (`dojo.legacy.crm`, a
library: do not change it) that answers with string maps:

| CRM key | Example | Meaning |
| --- | --- | --- |
| `CUST_NM` | `"HOPPER, GRACE BREWSTER"` | family name, comma, given names, in capitals |
| `STAT_CD` | `"A"`, `"S"`, `"X"` | active, suspended, closed |
| `CRDT_LMT` | `"000150000"` | credit limit in cents, zero-padded |
| `SEG` | `"01"`, `"02"` | retail, business |

Today `booking.domain.BookingService` reads those keys itself and posts to
`billing.internal.Ledger` directly. Fix both.

## The target design

| Type | What it does |
| --- | --- |
| `booking.domain.Customer` | a **record** `(String id, String displayName, Standing standing, long creditLimitCents, boolean business)` |
| `booking.domain.Standing` | enum `GOOD`, `SUSPENDED`, `CLOSED` |
| `booking.domain.CustomerDirectory` | port: `Optional<Customer> find(String customerId)` |
| `booking.acl.CrmCustomerDirectory` | `CrmCustomerDirectory(CrmClient crm)` implements the port: the **anti-corruption layer** |
| `billing.api.Billing` | `ChargeReceipt charge(ChargeRequest request)`, `List<ChargeReceipt> charges()` |
| `billing.internal.BillingService` | `BillingService()` implements `Billing` with the `Ledger` |
| `booking.domain.BookingService` | `BookingService(CustomerDirectory customers, Billing billing)`; `Booking book(String bookingId, String customerId, String vehicle, int days, long dailyRateCents)` |
| `dojo.rentals.App` | `static Rentals create(CrmClient crm)` |
| `dojo.rentals.Rentals` | a record `(BookingService booking, Billing billing)` |

The translation in the anti-corruption layer:

- no record (`fetchCustomer` returns `null`) → `Optional.empty()`;
- `displayName`: given names, then the family name, each word capitalised:
  `"HOPPER, GRACE BREWSTER"` → `"Grace Brewster Hopper"`, `"LOVELACE, ADA"` → `"Ada Lovelace"`;
- `STAT_CD` `A`, `S`, `X` → `GOOD`, `SUSPENDED`, `CLOSED`; any other code throws
  `IllegalStateException` (the CRM changed, and someone must look);
- `CRDT_LMT` → a `long`; `SEG` `"02"` → `business = true`, anything else `false`.

The booking rules, in order:

1. `days < 1` throws `IllegalArgumentException`; an unknown customer throws
   `IllegalArgumentException("unknown customer " + id)`;
2. a customer who is not `GOOD` is refused: `BookingRefusedException("customer is suspended")`
   or `"customer is closed"`;
3. the price is `days × dailyRateCents`, and business customers pay 90 % of it (`price * 9 / 10`,
   rounded down);
4. a price above the credit limit is refused: `BookingRefusedException("over credit limit")`;
5. billing is charged with `ChargeRequest(customerId, bookingId, price)`, and the `Booking` keeps
   the receipt's charge id. Refused bookings charge nothing.

`Ledger`, `ChargeRequest`, `ChargeReceipt`, `Booking` and `BookingRefusedException` keep their
shape. Charges are numbered `CH-1`, `CH-2`, ...

## What the grading tests check

- The graph: only `booking.acl` (and the composition root) uses `dojo.legacy`; `booking.domain`
  uses neither `booking.acl` nor anything of billing outside `billing.api`; billing uses nothing
  of booking.
- The translation, with a CRM the tests fill in, including names with several given names and an
  unknown status code.
- Every booking rule, and that refused bookings charge nothing.

The visible tests describe the target design, so they do not compile until it exists. Keep your
tests in the package `dojo`.
