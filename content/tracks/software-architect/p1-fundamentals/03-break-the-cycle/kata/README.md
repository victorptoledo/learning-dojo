# Break the Cycle

The shop has two packages, and they depend on each other:

```text
dojo.shop.orders  ──── OrderService calls InvoiceService ───▶  dojo.shop.billing
dojo.shop.orders  ◀─── InvoiceService reads Order, OrderLine ──  dojo.shop.billing
dojo.shop.orders  ◀─── PaymentService marks the Order paid ────  dojo.shop.billing
```

Run `ArchitectureTest`: `ClassGraph.packageDependencies` shows the cycle. Your job is to keep
**`orders -> billing`** and remove every use of `dojo.shop.orders` from `dojo.shop.billing`, so
billing could become a library (or a service) of its own.

## The target design

| Type | What it does |
| --- | --- |
| `billing.InvoiceRequest` | new **record** `(String orderId, String customer, long netCents)`: what billing needs to know, in billing's words |
| `billing.InvoiceService` | `InvoiceService()`; `Invoice invoice(InvoiceRequest request)` replaces `invoiceFor(Order)`; `find` and `all` stay |
| `billing.PaymentListener` | new **interface** with `void invoicePaid(String orderId)` |
| `billing.PaymentService` | `PaymentService(InvoiceService invoices, PaymentListener listener)`; `recordPayment` keeps its rules and then tells the listener |
| `orders.MarkOrderPaid` | new; `MarkOrderPaid(OrderRepository orders)` implements `PaymentListener` by marking the order paid |
| `orders.OrderService` | builds the `InvoiceRequest` from the order (net = sum of quantity × unit price) |
| `dojo.shop.App` | `create()` wires the same `Shop` as before, with a `MarkOrderPaid` as the listener |

The rules do not change:

- Invoices are numbered `INV-1`, `INV-2`, ... per `InvoiceService`. VAT is 20 % of the net,
  rounded half up to whole cents: `(net * 20 + 50) / 100`.
- Placing an order saves it, invoices it and leaves it `INVOICED` with the invoice id.
- `recordPayment(invoiceId, cents)`: an unknown invoice or an amount other than the invoice
  total throws `IllegalArgumentException`; paying an invoice twice throws
  `IllegalStateException`; otherwise the order becomes `PAID`. A rejected payment changes
  nothing.

## What the grading tests check

- The package graph, read from the compiled classes: billing uses nothing in orders, and there is
  no cycle between the shop's packages.
- `PaymentListener` is an interface in billing, and `PaymentService` works with any listener.
- Every rule above, through `App.create()` and through billing alone.

Keep your tests in the package `dojo`.
