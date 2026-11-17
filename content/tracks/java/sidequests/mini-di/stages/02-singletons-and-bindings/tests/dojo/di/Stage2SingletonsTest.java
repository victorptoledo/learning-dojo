package dojo.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Stage2SingletonsTest {

    @Singleton
    public static class Clock {
    }

    public static class Job {
        final Clock clock;

        public Job(Clock clock) {
            this.clock = clock;
        }
    }

    public interface Mailer {
        String send(String to);
    }

    public static class SmtpMailer implements Mailer {
        @Override
        public String send(String to) {
            return "smtp:" + to;
        }
    }

    @Singleton
    public static class SharedMailer implements Mailer {
        @Override
        public String send(String to) {
            return "shared:" + to;
        }
    }

    public static class Signup {
        final Mailer mailer;

        public Signup(Mailer mailer) {
            this.mailer = mailer;
        }
    }

    public abstract static class Repository {
    }

    public record Settings(String url) {
    }

    public static class Client {
        final Settings settings;

        public Client(Settings settings) {
            this.settings = settings;
        }
    }

    @Test
    void aSingletonIsBuiltOncePerContainer() {
        var container = new Container();
        container.register(Clock.class);
        container.register(Job.class);
        Clock clock = container.get(Clock.class);
        assertSame(clock, container.get(Clock.class));
        assertSame(clock, container.get(Job.class).clock);
        assertNotSame(container.get(Job.class), container.get(Job.class), "Job itself is not a singleton");
    }

    @Test
    void containersDoNotShareSingletons() {
        var first = new Container();
        var second = new Container();
        first.register(Clock.class);
        second.register(Clock.class);
        assertNotSame(first.get(Clock.class), second.get(Clock.class));
    }

    @Test
    void aBindingBuildsTheImplementation() {
        var container = new Container();
        container.bind(Mailer.class, SmtpMailer.class);
        container.register(Signup.class);
        assertEquals("smtp:ana", container.get(Signup.class).mailer.send("ana"));
        assertEquals(SmtpMailer.class, container.get(Mailer.class).getClass());
        assertNotSame(container.get(Mailer.class), container.get(Mailer.class));
    }

    @Test
    void aBoundImplementationKeepsItsScope() {
        var container = new Container();
        container.bind(Mailer.class, SharedMailer.class);
        assertSame(container.get(Mailer.class), container.get(Mailer.class));
        assertSame(container.get(Mailer.class), container.get(SharedMailer.class));
    }

    @Test
    void anInstanceIsAlwaysTheSameObject() {
        var container = new Container();
        var settings = new Settings("jdbc:postgresql://db/app");
        container.instance(Settings.class, settings);
        container.register(Client.class);
        assertSame(settings, container.get(Settings.class));
        assertSame(settings, container.get(Client.class).settings);
    }

    @Test
    void anInterfaceWithoutBindingIsAnError() {
        var container = new Container();
        container.register(Signup.class);
        var error = assertThrows(ContainerException.class, () -> container.get(Signup.class));
        assertTrue(error.getMessage().contains("no binding for") && error.getMessage().contains("Mailer"),
                error.getMessage());
        var direct = assertThrows(ContainerException.class, () -> container.get(Repository.class));
        assertTrue(direct.getMessage().contains("no binding for") && direct.getMessage().contains("Repository"),
                direct.getMessage());
    }
}
