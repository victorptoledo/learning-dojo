---
from: java
status: draft
concept: "Type transformations"
from_way: "Writing dozens of DTOs by hand for every variation of fields on a screen."
native_way: "`type UpdateUserDto = Partial<Omit<User, 'id'>>;` produces the right type in one line."
sensei_tip: "TypeScript's type system is Turing-complete: you can encode rules that validate route schemas while you type."
---
