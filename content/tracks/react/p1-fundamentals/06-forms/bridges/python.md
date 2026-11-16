---
from: python
status: draft
concept: "Django forms on the client"
from_way: "Django Forms or WTForms declare fields, clean() validates, and the template renders errors next to each field after a POST."
native_way: "In React you hold the values (controlled) or read them with FormData, validate with a pure function, and render errors next to the fields instantly."
sensei_tip: "form.is_valid() and form.errors map to validate(values) and an errors object; request.POST maps to FormData."
---

| Django | React |
| --- | --- |
| `request.POST["email"]` | `formData.get("email")` |
| `form.is_valid()` | `Object.keys(validate(values)).length === 0` |
| `form.errors["email"]` | `errors.email` |
| `{{ form.email.errors }}` | `{errors.email && <p role="alert">...}` |
| `initial=` | `defaultValue` |
| Server validation | Still required; client checks are for the user |
