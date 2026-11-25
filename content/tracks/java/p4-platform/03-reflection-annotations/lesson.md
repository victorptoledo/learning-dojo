# Annotations and Reflection

`@Test`, `@Override`, `@GetMapping`, `@Entity`: you have used annotations since the first
kata. This lesson opens the box. You will write your own annotation, read it at run time with
reflection, and see what frameworks such as JUnit and Spring do with it, and what it costs.

## An annotation is data about code

An annotation does nothing by itself. It is a typed label that a tool reads: the compiler
(`@Override`), an annotation processor at build time (Lombok, MapStruct), or your own code at
run time through reflection (JUnit, Spring, Jackson).

```java
@Retention(RetentionPolicy.RUNTIME)          // keep it in the class file and at run time
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD})
public @interface Column {
    String value();                          // @Column("hours") or @Column(value = "hours")
    boolean required() default true;
}
```

- **`@Retention`** decides who can see it: `SOURCE` (compiler only), `CLASS` (the default:
  in the class file, invisible to reflection) or `RUNTIME`. Forgetting `RUNTIME` is the
  classic reason a framework "ignores" your annotation.
- **`@Target`** decides where it may be written. On records, annotations on a component are
  copied to the places the target allows (the component, the field, the accessor, the
  constructor parameter).
- Elements are primitives, `String`, `Class`, enums, other annotations or arrays of these;
  defaults make them optional.

## Reflection: looking at a class at run time

`Class<?>` is the entry point. From it you reach methods, fields, constructors and, for
records, components:

```java
record Shift(@Column("person") String person, @Column("hours") int hours) {}

for (RecordComponent component : Shift.class.getRecordComponents()) {   // declaration order
    Column column = component.getAnnotation(Column.class);              // null when absent
    System.out.println(component.getName() + " <- " + column.value() + " : " + component.getType());
}
// person <- person : class java.lang.String
// hours <- hours : int
```

Reading a value and building an instance:

```java
Object hours = component.getAccessor().invoke(shift);          // calls shift.hours()

Class<?>[] types = Arrays.stream(Shift.class.getRecordComponents())
        .map(RecordComponent::getType).toArray(Class<?>[]::new);
Constructor<Shift> canonical = Shift.class.getDeclaredConstructor(types);
Shift made = canonical.newInstance("Ada", 8);                   // runs the compact constructor
```

Three things trip people up:

- **Primitive types are their own classes.** `int.class` is not `Integer.class`; a mapper
  that converts text must handle both.
- **Exceptions are wrapped.** A constructor that throws `IllegalArgumentException` reaches you
  as `InvocationTargetException`; the real error is `getCause()`. Unwrap it, or the user sees
  a useless message.
- **Access rules still apply.** `setAccessible(true)` on a private member of another module
  fails with `InaccessibleObjectException`; the module must `opens` the package. Public
  records and their accessors need no tricks.

Generic type arguments are erased from objects, but **declarations** keep them:
`field.getGenericType()` returns a `ParameterizedType` for `List<String>`, which is how
Jackson knows what to put in the list.

## What frameworks do with this

A test runner is a loop over reflection:

```java
for (Method method : testClass.getDeclaredMethods()) {
    if (method.isAnnotationPresent(Test.class)) {
        Object instance = testClass.getDeclaredConstructor().newInstance();
        method.invoke(instance);
    }
}
```

Spring does the same at a larger scale: it scans packages for `@Component`, reads constructor
parameter types to build the object graph, and wraps beans in proxies for `@Transactional`.
Knowing this explains most "magic" errors: a missing `RUNTIME`, a class not in the scanned
package, a proxy that a call from inside the same object skips.

## The costs

- **Speed.** Looking members up is slow; invoking through `Method` is slower than a direct
  call. Look up once and cache (`Map<Class<?>, Mapper>`), or turn it into a `MethodHandle`
  or a lambda when it sits on a hot path.
- **Safety.** Errors move from compile time to run time: a renamed accessor still compiles.
  Validate the annotations **once**, when you build the mapper, and fail with a clear message
  naming the class and the component.
- **Native images and shrinking.** Ahead-of-time compilation sees only what the code reaches
  directly. Reflection by name needs metadata (you will meet this in the Mastery phase).

Use reflection where the set of classes is open (a framework, a mapper for any record), and
plain code (a `switch`, a registry of lambdas) where the set is known.

## Summary

- An annotation is data; `@Retention(RUNTIME)` makes it visible to reflection, `@Target`
  says where it goes.
- `getRecordComponents()` returns components in declaration order, with their annotations,
  types and accessors; the canonical constructor takes the component types in that order.
- Unwrap `InvocationTargetException`; handle `int.class` and `Integer.class`.
- Look members up once, validate early, and prefer plain code when the set of types is closed.
