Scopes and interfaces.

- A class annotated `@Singleton` (new in the starter) is built **once** per container: every
  `get`, and every object that needs it, gets the same instance. Two containers never share
  instances.
- Code depends on interfaces; a binding says which class to build:

```java
public <T> void bind(Class<T> type, Class<? extends T> implementation)
public <T> void instance(Class<T> type, T object)
```

  `bind` makes `get(type)` build `implementation` (registering it too, keeping its own
  scope). `instance` hands over an object built elsewhere: `get(type)` always returns it.
- `get` of an interface or abstract class without a binding is a `ContainerException` whose
  message contains `no binding for` and its simple name.
