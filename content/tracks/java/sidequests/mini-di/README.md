# Sidequest: Mini dependency injection

Spring, Guice and Dagger all answer one question: *who builds my objects?* Build your own
small answer, `dojo.di.Container`, with annotations and reflection only (the JDK, no
libraries).

```java
var container = new Container();
container.register(Engine.class);
container.register(Car.class);
Car car = container.get(Car.class);   // new Car(new Engine())
```

The contract tests declare their own classes, annotate them with **your** annotations
(the starter gives you each one when its stage opens) and check what the container builds.
How you look up constructors, cache singletons or describe errors is yours. A container is a
good place for the patterns of the Design Patterns track: a registry, factories, a strategy
per scope, a proxy for lazy objects.

A few reflection tools you will need: `Class.getConstructors()`,
`Constructor.getParameters()`, `isAnnotationPresent`, `getGenericParameterTypes()` and
`ParameterizedType`, `InvocationTargetException.getCause()`.

Failures are a `ContainerException` (unchecked) with a message a person can act on.
