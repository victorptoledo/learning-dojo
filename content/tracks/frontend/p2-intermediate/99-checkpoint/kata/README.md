# Checkpoint 2: an Offline-Safe Feedback Form

`index.html` and `style.css` are done. Write **`app.js`**: a feedback form that explains its
errors accessibly, never loses what the user typed, and sends JSON to the server. The tests run
a fake `POST /api/feedback` that answers `201 { id }` (after 300 ms), or `503` when the name is
`Offline`.

The form has `novalidate`: the browser still evaluates `required`, `type="email"`, `minlength`
and the rest (the Constraint Validation API, `field.validity`), but it does not show its own
bubbles, so `app.js` shows the messages.

| Code | What it does |
| --- | --- |
| `errorFor(field)` | The message for a control, or `""`. See the table below. A blank value (only spaces) counts as missing. `validity.tooShort` is only set after the user edits a field, so also compare the trimmed length with `field.minLength` |
| `checkField(form, name)` | Puts `errorFor` of that field in `#NAME-error`; sets `aria-invalid="true"` on the control (on the `<fieldset>` for the rating) or removes it when valid. Returns `true` when valid |
| `validate(form)` | Runs `checkField` on name, email, rating and message; returns the first invalid control or `null` |
| `formValues(form)` | `{ name, email, rating, message }` from `new FormData(form)`, all strings (`""` when missing) |
| `saveDraft(form)` / `loadDraft(form)` | Store `formValues` as JSON in `localStorage` under `"feedback-draft"` / fill the form back from it. A draft that is not valid JSON is ignored |
| `sendFeedback(values)` | `POST /api/feedback` with a JSON body and `content-type: application/json`; returns the response JSON, throws an `Error` with the status in its message when not `ok` |

| Field | Missing | Other |
| --- | --- | --- |
| name | `Enter your name.` | |
| email | `Enter your email address.` | wrong format: `Enter an email address like name@example.com.` |
| rating | `Choose a rating.` | |
| message | `Write a message.` | fewer than 10 characters: `Write at least 10 characters.` |

Wiring:

- **On load**, restore the draft and set `#message-count` to `"N / 500"`.
- **On input** anywhere in the form, save the draft, update the counter, and re-check the field
  only if it is already marked invalid (do not scold someone who is still typing).
- **On submit** (`preventDefault`): if `validate` finds a problem, put
  `"Please fix the errors in the form."` in `#form-status` and focus the first invalid control.
  Otherwise disable the submit button, show `"Sending…"`, and send:
  - success: `"Thanks, NAME! Your feedback was sent."`, reset the form, delete the draft,
    reset the counter;
  - failure: `"Sending failed. Your message is saved; try again."` and keep everything;
  - always enable the button again.

There are 15 tests: 1 in `tests/visible.test.js` and 14 hidden ones.
