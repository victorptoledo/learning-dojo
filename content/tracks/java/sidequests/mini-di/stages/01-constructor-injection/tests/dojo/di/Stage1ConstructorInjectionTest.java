package dojo.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Stage1ConstructorInjectionTest {

    public static class Fuel {
    }

    public static class Engine {
        final Fuel fuel;

        public Engine(Fuel fuel) {
            this.fuel = fuel;
        }
    }

    public static class Wheels {
    }

    public static class Car {
        final Engine engine;
        final Wheels wheels;

        public Car(Engine engine, Wheels wheels) {
            this.engine = engine;
            this.wheels = wheels;
        }
    }

    public static class TwoWays {
        final String how;

        public TwoWays() {
            this.how = "default";
        }

        @Inject
        public TwoWays(Fuel fuel) {
            this.how = "injected";
        }
    }

    public static class Ambiguous {
        public Ambiguous() {
        }

        public Ambiguous(Fuel fuel) {
        }
    }

    public static class Broken {
        public Broken() {
            throw new IllegalStateException("no config file");
        }
    }

    public static class NeedsBroken {
        public NeedsBroken(Broken broken) {
        }
    }

    private Container containerWith(Class<?>... types) {
        var container = new Container();
        for (Class<?> type : types) {
            container.register(type);
        }
        return container;
    }

    @Test
    void buildsAClassWithoutDependencies() {
        assertEquals(Fuel.class, containerWith(Fuel.class).get(Fuel.class).getClass());
    }

    @Test
    void buildsTheWholeGraphThroughConstructors() {
        var container = containerWith(Fuel.class, Engine.class, Wheels.class, Car.class);
        Car car = container.get(Car.class);
        assertEquals(Engine.class, car.engine.getClass());
        assertEquals(Fuel.class, car.engine.fuel.getClass());
        assertEquals(Wheels.class, car.wheels.getClass());
    }

    @Test
    void everyGetBuildsNewObjects() {
        var container = containerWith(Fuel.class, Engine.class, Wheels.class, Car.class);
        Car first = container.get(Car.class);
        Car second = container.get(Car.class);
        assertNotSame(first, second);
        assertNotSame(first.engine, second.engine);
    }

    @Test
    void registrationOrderDoesNotMatter() {
        var container = containerWith(Car.class, Wheels.class, Engine.class, Fuel.class);
        assertSame(Car.class, container.get(Car.class).getClass());
    }

    @Test
    void anUnregisteredClassIsAnError() {
        var container = containerWith(Engine.class);
        var direct = assertThrows(ContainerException.class, () -> container.get(Wheels.class));
        assertTrue(direct.getMessage().contains("not registered") && direct.getMessage().contains("Wheels"),
                direct.getMessage());
        var nested = assertThrows(ContainerException.class, () -> container.get(Engine.class));
        assertTrue(nested.getMessage().contains("not registered") && nested.getMessage().contains("Fuel"),
                nested.getMessage());
    }

    @Test
    void injectPicksTheConstructor() {
        var container = containerWith(Fuel.class, TwoWays.class);
        assertEquals("injected", container.get(TwoWays.class).how);
    }

    @Test
    void severalConstructorsWithoutInjectAreAnError() {
        var container = containerWith(Fuel.class, Ambiguous.class);
        var error = assertThrows(ContainerException.class, () -> container.get(Ambiguous.class));
        assertTrue(error.getMessage().contains("constructor"), error.getMessage());
    }

    @Test
    void aThrowingConstructorKeepsItsCause() {
        var container = containerWith(Broken.class, NeedsBroken.class);
        var error = assertThrows(ContainerException.class, () -> container.get(NeedsBroken.class));
        assertEquals(IllegalStateException.class, error.getCause().getClass());
        assertEquals("no config file", error.getCause().getMessage());
    }
}
