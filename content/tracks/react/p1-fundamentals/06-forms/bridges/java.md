---
from: java
status: draft
concept: "Form binding without a framework"
from_way: "Spring MVC binds request parameters to a command object with @ModelAttribute and validates it with Bean Validation (@NotBlank, @Email), returning errors to the view."
native_way: "A controlled form keeps the command object in state; a pure validate function returns errors; the submit handler or a form action sends it."
sensei_tip: "Write validate(values) like a Bean Validation validator: pure, testable, returning a map of field to message, and run it again on the server."
---

| Spring | React |
| --- | --- |
| `@ModelAttribute SignupForm form` | `const [values, setValues] = useState<SignupForm>(...)` |
| `@NotBlank`, `@Email` | `validate(values)` returning errors |
| `BindingResult` | an `errors` object in state |
| `th:errors="*{email}"` | `<p id={errorId} role="alert">{errors.email}</p>` |
| POST handler | `onSubmit` or `<form action={fn}>` |
| CSRF token, server validation | Still needed: the client is not trusted |
