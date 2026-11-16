---
from: go
status: draft
concept: "r.FormValue on the client"
from_way: "An HTTP handler calls r.ParseForm and r.FormValue(\"email\"), validates, and re-renders the template with errors."
native_way: "React reads the same values from FormData or from state, validates in a pure function, and shows errors without a round trip; the server still validates."
sensei_tip: "Keep the Go handler's validation as the authority; the React validate() is a copy for fast feedback, with the same rules."
---

| Go | React |
| --- | --- |
| `r.FormValue("email")` | `formData.get("email")` |
| `if email == "" { errs["email"] = ... }` | `validate(values)` |
| Re-render template with errors | `errors` state rendered next to fields |
| `http.Redirect` after POST | Update state, or let the action return a result |
| CSRF middleware | Still needed on the server |
| `<label for>` | `<label htmlFor>` |
