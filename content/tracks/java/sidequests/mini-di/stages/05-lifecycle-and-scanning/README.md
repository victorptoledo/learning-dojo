Objects that start and stop, and a container that finds its classes alone.

- After the container builds an object, it calls its public no-argument methods annotated
  `@Init` (new in the starter). An `@Init` that throws makes the `get` fail, like a throwing
  constructor.
- `Container` is `AutoCloseable`, with a `public void close()` that throws no checked
  exception (so `try (var container = new Container()) { ... }` needs no `catch`). It calls the public no-argument `@Destroy` methods of
  the **singletons** it built, in the reverse order they were built (dependencies are built
  first, so they are destroyed last). Prototypes are not tracked. After `close`, `get` is a
  `ContainerException` whose message contains `closed`; closing twice does nothing.
- `scan(String packageName)` registers every class annotated `@Component` in that package and
  the packages below it, as found on the classpath (folders of `.class` files are enough).
  Classes without `@Component` are left alone. `@Singleton` still applies to scanned classes.
