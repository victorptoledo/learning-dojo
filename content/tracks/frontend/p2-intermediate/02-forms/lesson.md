# Forms: Native Controls, Constraint Validation and FormData

Forms are where users give you data: sign-ups, searches, checkouts. The platform does a lot for
free: labelled controls, keyboard support, autofill, mobile keyboards, validation and
submission. Start from that, then improve it with JavaScript.

## The right control for the data

| Data | Control | What you get |
| --- | --- | --- |
| Email | `<input type="email" autocomplete="email">` | Email keyboard on phones, format check, autofill |
| Phone | `type="tel" autocomplete="tel"` | Number pad (no format check: formats vary) |
| Number | `type="number" min max step` or `inputmode="numeric"` | Number keyboard; use `inputmode` with `type="text"` for card numbers and codes |
| Date | `type="date"` | A native date picker |
| One of few options | Radio buttons in a `<fieldset>` with `<legend>` | One choice, arrow keys move between them |
| One of many | `<select>` (or `<input list>` + `<datalist>`) | A compact list |
| Yes/no | `<input type="checkbox">` | A toggle |
| Long text | `<textarea>` | Multi-line input |
| Password | `type="password" autocomplete="current-password"` / `new-password` | Hidden text, password managers |

Every control needs a `<label>`; `name` is the key it is sent under. `autocomplete` tokens let
browsers and password managers fill in forms correctly, and help users with motor or memory
difficulties (WCAG 1.3.5).

## Submission without JavaScript

```html
<form action="/subscribe" method="post">
  <label for="email">Email</label>
  <input id="email" name="email" type="email" required>
  <button>Subscribe</button>
</form>
```

Enter in a field submits the form; the browser sends `email=...` and loads the response. That
baseline still works if your script fails to load: **progressive enhancement**.

## Constraint validation

Attributes declare the rules; the browser checks them:

| Attribute | `validity` flag |
| --- | --- |
| `required` | `valueMissing` |
| `type="email"`, `type="url"` | `typeMismatch` |
| `minlength`, `maxlength` | `tooShort`, `tooLong` |
| `min`, `max`, `step` | `rangeUnderflow`, `rangeOverflow`, `stepMismatch` |
| `pattern="[A-Z]{3}"` | `patternMismatch` |
| `setCustomValidity("...")` | `customError` |

```js
input.validity.valueMissing;     // one flag per rule
input.validationMessage;          // the browser's (localized) message
input.checkValidity();            // true/false, fires "invalid" when false
form.reportValidity();            // also shows the browser's messages
input.setCustomValidity("Write a task first."); // "" clears it again
```

CSS can react: `input:invalid`, `:user-invalid` (only after the user interacted),
`:required`, `:placeholder-shown`.

## Custom error messages, accessibly

The browser's bubbles are hard to style and vanish quickly. Many products show their own:

```html
<form novalidate>                                   <!-- keep the rules, hide the bubbles -->
  <label for="email">Email</label>
  <input id="email" name="email" type="email" required aria-describedby="email-error">
  <p id="email-error" class="error"></p>
</form>
```

```js
function check(input) {
  const message = input.validity.valueMissing ? "Enter your email address."
    : input.validity.typeMismatch ? "Enter an email address like name@example.com." : "";
  document.getElementById(`${input.id}-error`).textContent = message;
  input.toggleAttribute("aria-invalid", message !== "");
  return message === "";
}
```

Good error handling:

- Validate on **submit** first; then re-check a field **as the user fixes it**. Do not shout
  "invalid email" at the first keystroke.
- Say what is wrong **and** how to fix it, next to the field, tied with `aria-describedby`,
  marked with `aria-invalid="true"`, not only in red.
- Move the focus to the **first** invalid field, and announce a summary if there are many.
- Client-side validation is for the user's convenience: the **server must validate again**,
  because anyone can send any request.

## Reading values: FormData

```js
form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const data = new FormData(form);           // every named, enabled control
  data.get("email");                          // "ada@example.com"
  data.getAll("topics");                      // checkboxes with the same name
  const values = Object.fromEntries(data);    // a plain object
  await fetch("/api/subscribe", { method: "POST", body: data });               // multipart
  await fetch("/api/subscribe", { method: "POST", body: new URLSearchParams(data) }); // urlencoded
  await fetch("/api/subscribe", { method: "POST", headers: { "content-type": "application/json" },
    body: JSON.stringify(values) });                                            // JSON
});
```

Unchecked checkboxes and radios are **absent** from FormData; disabled controls too.

## While sending

Disable the submit button (or ignore repeated submits) while a request is in flight, show
progress in a live region ("Sending…"), and keep what the user typed if sending fails.

## Summary

- Pick the input type and `autocomplete` token that match the data; label everything.
- A plain form submits without JavaScript; enhance from there.
- Constraint attributes plus `validity` give you validation; `setCustomValidity` adds rules.
- Show errors next to fields with `aria-describedby` and `aria-invalid`, focus the first one.
- `FormData` reads a whole form; the server validates again.

## Further reading

- MDN, "Web forms": <https://developer.mozilla.org/en-US/docs/Learn_web_development/Extensions/Forms>
- MDN, "Client-side form validation": <https://developer.mozilla.org/en-US/docs/Learn_web_development/Extensions/Forms/Form_validation>
- MDN, `ValidityState`: <https://developer.mozilla.org/en-US/docs/Web/API/ValidityState>
- MDN, `FormData`: <https://developer.mozilla.org/en-US/docs/Web/API/FormData>
- WHATWG HTML, "Forms" and "Autofill": <https://html.spec.whatwg.org/multipage/forms.html>
- W3C WAI, "Forms Tutorial": <https://www.w3.org/WAI/tutorials/forms/>
- web.dev, "Learn Forms": <https://web.dev/learn/forms>
