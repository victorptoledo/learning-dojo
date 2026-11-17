```java
public void register(Class<?> type)
public <T> T get(Class<T> type)
```

- `get` builds a registered class by calling its constructor, and builds every constructor
  parameter the same way, recursively.
- Which constructor: the one annotated `@Inject` (in the starter); without `@Inject`, the only
  public constructor. A class with several public constructors and none marked `@Inject` is a
  `ContainerException` whose message contains `constructor`.
- Every `get` builds new objects (singletons come in stage 2).
- Asking for a class that is not registered, directly or as a parameter, is a
  `ContainerException` whose message contains `not registered` and the class's simple name.
- A constructor that throws is a `ContainerException` whose **cause** is what the constructor
  threw (not the `InvocationTargetException` around it).
