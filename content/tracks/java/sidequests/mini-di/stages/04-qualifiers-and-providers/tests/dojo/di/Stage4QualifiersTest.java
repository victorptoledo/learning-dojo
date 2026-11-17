package dojo.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class Stage4QualifiersTest {

    public interface Store {
        String name();
    }

    public static class MemoryStore implements Store {
        @Override
        public String name() {
            return "memory";
        }
    }

    public static class DiskStore implements Store {
        @Override
        public String name() {
            return "disk";
        }
    }

    public static class Service {
        final Store cache;
        final Store archive;
        final Store plain;

        public Service(@Named("cache") Store cache, @Named("archive") Store archive, Store plain) {
            this.cache = cache;
            this.archive = archive;
            this.plain = plain;
        }
    }

    static final AtomicInteger REPORTS = new AtomicInteger();

    public static class Report {
        public Report() {
            REPORTS.incrementAndGet();
        }
    }

    @Singleton
    public static class Config {
    }

    public static class Lazy {
        final Provider<Report> reports;
        final Provider<Config> config;

        public Lazy(Provider<Report> reports, Provider<Config> config) {
            this.reports = reports;
            this.config = config;
        }
    }

    public static class NamedLazy {
        final Provider<Store> archive;

        public NamedLazy(@Named("archive") Provider<Store> archive) {
            this.archive = archive;
        }
    }

    @Singleton
    public static class Parent {
        final Provider<Child> child;

        public Parent(Provider<Child> child) {
            this.child = child;
        }
    }

    public static class Child {
        final Parent parent;

        public Child(Parent parent) {
            this.parent = parent;
        }
    }

    @Test
    void namedBindingsChooseTheImplementation() {
        var container = new Container();
        container.bind(Store.class, "cache", MemoryStore.class);
        container.bind(Store.class, "archive", DiskStore.class);
        container.bind(Store.class, MemoryStore.class);
        container.register(Service.class);
        Service service = container.get(Service.class);
        assertEquals("memory", service.cache.name());
        assertEquals("disk", service.archive.name());
        assertEquals("memory", service.plain.name());
        assertEquals("disk", container.get(Store.class, "archive").name());
    }

    @Test
    void aMissingNameIsAnError() {
        var container = new Container();
        container.bind(Store.class, MemoryStore.class);
        container.register(Service.class);
        var error = assertThrows(ContainerException.class, () -> container.get(Service.class));
        assertTrue(error.getMessage().contains("no binding for Store named cache"), error.getMessage());
        var direct = assertThrows(ContainerException.class, () -> container.get(Store.class, "nope"));
        assertTrue(direct.getMessage().contains("no binding for Store named nope"), direct.getMessage());
    }

    @Test
    void aProviderBuildsOnlyWhenAsked() {
        var container = new Container();
        container.register(Report.class);
        container.register(Config.class);
        container.register(Lazy.class);
        int before = REPORTS.get();
        Lazy lazy = container.get(Lazy.class);
        assertEquals(before, REPORTS.get(), "nothing built yet");
        Report first = lazy.reports.get();
        Report second = lazy.reports.get();
        assertNotSame(first, second, "a prototype is new each time");
        assertEquals(before + 2, REPORTS.get());
        assertSame(lazy.config.get(), lazy.config.get(), "a singleton stays one");
        assertSame(container.get(Config.class), lazy.config.get());
    }

    @Test
    void aNamedProviderUsesTheNamedBinding() {
        var container = new Container();
        container.bind(Store.class, "archive", DiskStore.class);
        container.register(NamedLazy.class);
        assertEquals("disk", container.get(NamedLazy.class).archive.get().name());
    }

    @Test
    void aProviderBreaksACycle() {
        var container = new Container();
        container.register(Parent.class);
        container.register(Child.class);
        Parent parent = container.get(Parent.class);
        Child child = parent.child.get();
        assertSame(parent, child.parent);
    }
}
