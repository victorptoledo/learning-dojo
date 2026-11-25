# Checkpoint 4: Invoices Go to Production

An invoicing service passed its demo. A clerk issues invoices, reads them back as JSON or as
printable text, and the notifications module plans a payment reminder for each one. Everything
works on a laptop. Now it faces the production review, and every point below must be fixed.

Two modules live in the application:

- `dojo.billing` is billing's **public** package: `Billing`, the records, `InvoiceIssued`,
  `InvoiceFormatter` and `BillingRuntimeHints`. `dojo.billing.internal` (the `Invoice` entity,
  its repository and the formatters) belongs to billing alone.
- `dojo.notifications` plans reminders (`Reminders`, `GET /reminders`).

Two users exist (see `UsersConfig`): `clerk` / `clerk-pass` with role `USER`, and
`admin` / `admin-pass` with roles `USER` and `ADMIN`.

| Request | Who | Answer |
| --- | --- | --- |
| `POST /invoices` `{"customer": "Globex", "amountCents": 125000}` | a user | `201`, the invoice (`ACME-0001`, issued today, due after the payment term) |
| `GET /invoices/{number}` and `GET /invoices/{number}/text` | a user | `200`, the invoice as JSON or as text |
| `GET /reminders` | a user | `200`, the planned reminders (`remindOn` is 3 days before `dueOn`) |
| `POST /admin/maintenance` `{"on": true}` | an admin | `204`; you write this one |

## The review

1. **Settings fail fast.** `Billing` reads its settings with `@Value` and silent defaults. Replace
   them with a typed `BillingProperties` record bound to `billing.*` and validated at startup:
   `invoicePrefix` is 2 to 5 capital letters, `paymentTermDays` is from 1 to 90, and
   `formatter` is not blank. A wrong value must **stop the application at startup**, with the
   name of the setting in the error.
2. **Actuator shows only what the platform needs.** Expose `health`, `info` and `metrics` and
   nothing else (`env`, `beans`, `configprops`, `heapdump` ... answer `404`). Health and info are
   public, but only an **admin** sees the health details and components; anyone else gets just
   the status. Every other actuator path needs role `ADMIN` (`401` without a login, `403` for a
   clerk).
3. **Probes and a maintenance switch.** Boot 4 already serves the liveness and readiness
   probes (`/actuator/health/liveness`, `/actuator/health/readiness`); make both public. Write
   `POST /admin/maintenance` (admins only, `204`): `{"on": true}` takes the service **out of
   rotation**, so readiness answers `503` `OUT_OF_SERVICE` while liveness stays `200`, and
   `{"on": false}` brings it back.
4. **Modules talk through events.** Today `Billing` calls `Reminders` directly and `Reminders`
   reads billing's `InvoiceRepository`. Instead, `Billing` publishes `InvoiceIssued` and
   `Reminders` listens to it, using only the event's data, **after the invoice commits**: an
   invoice that rolls back gets no reminder. No class in `dojo.notifications` may name a type
   from `dojo.billing.internal`, and nothing in billing may name a type from
   `dojo.notifications` (in fields, constructors, methods or supertypes).
5. **Ready for a native image.** `Billing` creates the formatter named in `billing.formatter`
   by reflection and reads `billing/terms.txt` from the classpath. A native image keeps neither
   unless it is told. Fill in `BillingRuntimeHints`: both formatters with their public
   constructors, and the resource. Register it with `@ImportRuntimeHints`.

> Tip: `@ConfigurationProperties`, `@Validated`, `@ConfigurationPropertiesScan`,
> `management.endpoints.web.exposure.include`, `management.endpoint.health.show-details`,
> `management.endpoint.health.roles`, `AvailabilityChangeEvent` and `ReadinessState`, `@TransactionalEventListener`,
> `RuntimeHintsRegistrar` and `MemberCategory`.
