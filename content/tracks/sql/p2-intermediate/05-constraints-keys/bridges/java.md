---
from: java
status: draft
concept: "Rules the data itself enforces"
from_way: "Bean Validation (@NotNull, @Size, @Positive), checks in service code, and JPA @Column(unique = true)."
native_way: "NOT NULL, CHECK, UNIQUE, PRIMARY KEY, FOREIGN KEY and EXCLUDE, checked by the database on every write."
sensei_tip: "Keep Bean Validation for friendly messages, and the constraint for the guarantee. Catch the SQLSTATE (23505, 23503) and translate it, instead of checking first."
---

| Java | SQL |
| --- | --- |
| @NotNull | NOT NULL |
| @Positive | CHECK (seats > 0) |
| if (repo.existsByEmail(e)) throw ... | UNIQUE (email), and catch 23505 |
| @ManyToOne(optional = false) | NOT NULL REFERENCES customers (id) |
| CascadeType.REMOVE | ON DELETE CASCADE |
| DataIntegrityViolationException | SQLSTATE class 23 |
