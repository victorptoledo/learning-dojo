---
from: java
status: draft
concept: "Validation at the edge"
from_way: "Bean Validation annotations (@NotBlank, @Email, @Size) checked by Spring on the server."
native_way: "HTML constraint attributes (required, type=email, minlength) checked by the browser, with ValidityState flags per rule."
sensei_tip: "Mirror your @Size and @Email annotations as HTML attributes for fast feedback, and keep Bean Validation: the server check is the one that counts."
---

| Java | In the browser |
| --- | --- |
| `@NotBlank` | `required` (plus a trim check) |
| `@Email` | `type="email"` |
| `@Size(min = 10)` | `minlength="10"` |
| `BindingResult.getFieldErrors()` | `field.validity` and `validationMessage` |
