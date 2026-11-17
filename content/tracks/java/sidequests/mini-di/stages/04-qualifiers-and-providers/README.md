Two implementations of one interface, and objects you want later.

- Named bindings choose between implementations of the same type:

```java
public <T> void bind(Class<T> type, String name, Class<? extends T> implementation)
public <T> T get(Class<T> type, String name)
```

  A constructor parameter annotated `@Named("fast")` (new in the starter) gets the binding of
  its type with that name. A missing name is a `ContainerException` whose message contains
  `no binding for <Type> named <name>`. Unnamed parameters keep using the unnamed binding.
- A constructor parameter of type `Provider<T>` (new in the starter) gets a provider instead of
  a `T`: each `provider.get()` asks the container for a `T` at that moment, with its scope (a
  singleton stays one; a prototype is new each time). `@Named` works on a `Provider<T>`
  parameter too.
- A provider is lazy, so it breaks cycles: `A(Provider<B>)` and `B(A)` can both be built.
