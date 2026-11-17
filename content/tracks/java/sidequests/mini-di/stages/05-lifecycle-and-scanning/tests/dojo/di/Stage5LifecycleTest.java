package dojo.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.di.scan.Greeter;
import dojo.di.scan.NotAComponent;
import dojo.di.scan.deep.Counter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class Stage5LifecycleTest {

    static final List<String> EVENTS = new ArrayList<>();

    @Singleton
    public static class Database {
        boolean open;

        @Init
        public void connect() {
            open = true;
            EVENTS.add("connect database");
        }

        @Destroy
        public void disconnect() {
            EVENTS.add("disconnect database");
        }
    }

    @Singleton
    public static class Cache {
        final Database database;

        public Cache(Database database) {
            this.database = database;
        }

        @Destroy
        public void flush() {
            EVENTS.add("flush cache");
        }
    }

    public static class Request {
        boolean started;

        @Init
        public void start() {
            started = true;
        }

        @Destroy
        public void never() {
            EVENTS.add("destroy request");
        }
    }

    public static class BadInit {
        @Init
        public void fail() {
            throw new IllegalStateException("cannot start");
        }
    }

    @Test
    void initRunsOnceTheObjectIsBuilt() {
        var container = new Container();
        container.register(Database.class);
        container.register(Request.class);
        assertTrue(container.get(Database.class).open);
        assertTrue(container.get(Request.class).started, "prototypes get @Init too");
    }

    @Test
    void aThrowingInitFailsTheGet() {
        var container = new Container();
        container.register(BadInit.class);
        var error = assertThrows(ContainerException.class, () -> container.get(BadInit.class));
        assertEquals("cannot start", error.getCause().getMessage());
    }

    @Test
    void closeDestroysSingletonsInReverseOrder() {
        EVENTS.clear();
        var container = new Container();
        container.register(Database.class);
        container.register(Cache.class);
        container.register(Request.class);
        container.get(Cache.class);
        container.get(Request.class);
        container.close();
        assertEquals(List.of("connect database", "flush cache", "disconnect database"), EVENTS);
    }

    @Test
    void aClosedContainerRefusesToBuild() {
        var container = new Container();
        container.register(Database.class);
        container.close();
        container.close();
        var error = assertThrows(ContainerException.class, () -> container.get(Database.class));
        assertTrue(error.getMessage().contains("closed"), error.getMessage());
    }

    @Test
    void tryWithResourcesClosesTheContainer() {
        EVENTS.clear();
        try (var container = new Container()) {
            container.register(Database.class);
            container.get(Database.class);
        }
        assertEquals(List.of("connect database", "disconnect database"), EVENTS);
    }

    @Test
    void scanRegistersTheComponentsOfAPackageAndBelow() {
        var container = new Container();
        container.scan("dojo.di.scan");
        Greeter greeter = container.get(Greeter.class);
        assertEquals("hello ana (1)", greeter.greet("ana"));
        assertSame(container.get(Counter.class), container.get(Counter.class), "@Singleton still applies");
        var error = assertThrows(ContainerException.class, () -> container.get(NotAComponent.class));
        assertTrue(error.getMessage().contains("not registered"), error.getMessage());
    }
}
