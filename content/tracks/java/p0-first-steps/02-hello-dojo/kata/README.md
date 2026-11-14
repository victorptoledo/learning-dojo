# Hello, Dojo

Write the method `Greeter.greet` in `src/main/java/dojo/Greeter.java`. It receives a name
and returns a welcome message.

```text
greet("Ada")       ->  "Welcome to the dojo, Ada!"
greet("  Ada ")    ->  "Welcome to the dojo, Ada!"      (spaces around the name are ignored)
greet("")          ->  "Welcome to the dojo, stranger!" (no name: greet a stranger)
greet("   ")       ->  "Welcome to the dojo, stranger!" (only spaces counts as no name)
greet(null)        ->  "Welcome to the dojo, stranger!" (null counts as no name too)
```

What you start with:

```java
public static String greet(String name) {
    throw new UnsupportedOperationException("todo greet " + name);
}
```

Delete the `throw` line and write the body. Tools you will need:

- `name == null` checks for a missing reference; check it **before** calling a method on `name`.
- `name.strip()` returns the text without the spaces at both ends.
- `text.isEmpty()` is true when the text has no characters (`isBlank()` also accepts only spaces).
- `"Welcome to the dojo, %s!".formatted(who)` or `"Welcome to the dojo, " + who + "!"`.
- The ternary `condition ? a : b` produces `a` or `b`, so `String who = ... ? ... : ...;` works.

There are 6 tests: 1 in `src/test/java/dojo/GreeterTest.java` (you can read and run it) and
5 hidden ones that check the rules above.
