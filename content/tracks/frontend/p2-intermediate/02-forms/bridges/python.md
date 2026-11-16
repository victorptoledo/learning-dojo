---
from: python
status: draft
concept: "Validation at the edge"
from_way: "Django forms or Pydantic models that validate request data and collect errors per field."
native_way: "The browser validates declared constraints before sending; your script shows the messages and FormData reads the values."
sensei_tip: "Keep the Django form or Pydantic model as the source of truth, and render matching HTML attributes so the browser gives instant feedback."
---

| Python | In the browser |
| --- | --- |
| `forms.EmailField(required=True)` | `<input type="email" required>` |
| `form.errors["email"]` | `#email-error` tied with `aria-describedby` |
| `request.POST` | `new FormData(form)` |
| `model_validate(data)` on the server | Still needed: the server validates again |
