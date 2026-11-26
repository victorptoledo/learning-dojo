# Final Exam: the Cart on Release Day

The Kata Shop's cart page goes out today. `index.html` is a plain server-rendered form, and it
must stay one: without JavaScript a visitor changes the numbers, presses **Update cart**, and
pays by bank transfer. With JavaScript the page gets better: − and + buttons, a live total in
the visitor's language, errors reported to the team with the release that caused them, and card
payments from a provider's script that can be slow or down.

`index.html`, `style.css` and `vendor/pay.js` are done. Write **`quantity-input.js`** and
**`app.js`**. The tests run a fake `POST /api/errors` and serve `vendor/pay.js` normally, with
a `500`, or 1.5 s late.

## `quantity-input.js`: a progressively enhanced custom element

`<quantity-input>` wraps a `<label>` and an `<input type="number" min max>` that already work
on their own. It uses no shadow DOM, so the label keeps naming the input and the form keeps
submitting it. Export the class `QuantityInput` and register it as `quantity-input` unless
`customElements.get("quantity-input")` already has an element.

| Part | What it does |
| --- | --- |
| `connectedCallback()` | Finds the number input and adds two `<button type="button">`: one before it with `aria-label` `"Decrease LABEL"` and text `−`, one after it with `"Increase LABEL"` and text `+`, where LABEL is the input's label text (`"Dojo Tee quantity"`). The element can be moved in the page, and then the callback runs again: add the buttons only once |
| Buttons | Step the quantity by 1. `−` is `disabled` at `min`, `+` at `max` |
| Typing | On the input's `change`, clamp the value between `min` and `max` (empty or not a number becomes `min`) |
| Event | Whenever a click or a typed change gives a **new** value, dispatch `quantity-change` with `bubbles: true` and `detail: { name, value }` (the input's name, the number). No event when the value stays the same |
| `value` | Getter: the quantity as a number. Setter: clamps and updates the input and the buttons, without an event (like a native input's `value`) |

## `app.js`: production concerns

| Code | What it does |
| --- | --- |
| `formatPrice(cents, currency, locale)` | `Intl.NumberFormat` with `style: "currency"`: `"€54.00"` for en-IE, `"1.234,56 €"` for de-DE |
| `itemsLabel(count, locale)` | The count formatted for the locale and the word chosen with `Intl.PluralRules`: `"1 item"`, `"1,000 items"`; Portuguese (`pt`) says `"item"` / `"itens"`; other languages use the English words |
| `cartSummary(form)` | `{ count, cents }`: the sum of the quantities and of `data-price` × quantity over the `.item`s |
| `updateSummary(form)` | Writes `itemsLabel` into `#count` and `formatPrice` into `#total`, with the `<html lang>` and the form's `data-currency` |
| `reportProblem(kind, message)` | `navigator.sendBeacon` to `/api/errors` with the JSON `{ kind, message, release, page }`: `release` is the `<meta name="release">` content, `page` is `location.pathname`. A message already reported is not sent again, and a page sends at most **5** reports. Returns `true` only when it sent one |
| `installErrorReporting()` | Reports uncaught errors (`error` event, kind `"error"`) and unhandled promise rejections (`unhandledrejection`, kind `"rejection"`) with their messages |
| `loadScript(src, timeoutMs)` | Adds a `<script>` and resolves on `load`; rejects on `error` or when `timeoutMs` passes first |
| `setupPayments()` | Shows `"Loading card payments…"` in `#payment-status` and hides `#bank-transfer`. Loads `/vendor/pay.js` with a 3 s timeout and calls `window.Pay.mount(#payment)`, then empties the status. When the script fails or is too slow: `"Card payments are unavailable right now."`, show the bank transfer again and report it with kind `"payment"`. A script that arrives after you gave up must not mount |

On load, `app.js` installs the error reporting first, hides **Update cart** (the total is now
live), updates the summary on every `quantity-change` from the form, and sets up the payments.

The tests use roles and labels, as a user would: if a test cannot find "Increase Dojo Tee
quantity", check the button's accessible name. The slow-script test runs the page's clock
forward instead of waiting 3 real seconds, so use `setTimeout` for the timeout.

There are 17 tests: 1 in `tests/visible.test.js` and 16 hidden ones.
