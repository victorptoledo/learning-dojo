---
from: csharp
status: draft
concept: "EditForm and validation messages"
from_way: "Blazor's EditForm binds @bind-Value to a model, DataAnnotationsValidator checks attributes, and ValidationMessage shows errors."
native_way: "React has no built-in form component: value and onChange bind fields, a validate function or schema checks, and you render the messages."
sensei_tip: "@bind-Value is value plus onChange written out by hand; [Required] becomes a line in validate(); ValidationMessage becomes a <p> tied with aria-describedby."
---

| Blazor | React |
| --- | --- |
| `<EditForm Model="model" OnValidSubmit="Save">` | `<form onSubmit={handleSubmit}>` |
| `@bind-Value="model.Email"` | `value={email} onChange={(e) => setEmail(e.target.value)}` |
| `[Required]`, `[EmailAddress]` | `validate(values)` |
| `<ValidationMessage For="...">` | `<p role="alert">{errors.email}</p>` |
| Server-side `ModelState` | Server validation, still required |
| `InputCheckbox` | `checked` + `onChange` |
