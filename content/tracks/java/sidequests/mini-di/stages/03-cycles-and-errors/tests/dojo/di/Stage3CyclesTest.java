package dojo.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class Stage3CyclesTest {

    public static class Alpha {
        public Alpha(Beta beta) {
        }
    }

    public static class Beta {
        public Beta(Gamma gamma) {
        }
    }

    public static class Gamma {
        public Gamma(Alpha alpha) {
        }
    }

    public static class Narcissus {
        public Narcissus(Narcissus self) {
        }
    }

    public static class Top {
        public Top(Middle middle) {
        }
    }

    public static class Middle {
        public Middle(Bottom bottom) {
        }
    }

    public static class Bottom {
        public Bottom(Missing missing) {
        }
    }

    public static class Missing {
    }

    public static class Exploding {
        public Exploding() {
            throw new IllegalArgumentException("bad port");
        }
    }

    public static class UsesExploding {
        public UsesExploding(Exploding exploding) {
        }
    }

    static final AtomicInteger BUILT = new AtomicInteger();

    @Singleton
    public static class Flaky {
        public Flaky(Unready unready) {
            BUILT.incrementAndGet();
        }
    }

    public static class Unready {
        static boolean ready;

        public Unready() {
            if (!ready) {
                throw new IllegalStateException("not yet");
            }
        }
    }

    private static Container containerWith(Class<?>... types) {
        var container = new Container();
        for (Class<?> type : types) {
            container.register(type);
        }
        return container;
    }

    @Test
    void aCycleIsReportedWithItsPath() {
        var container = containerWith(Alpha.class, Beta.class, Gamma.class);
        var error = assertThrows(ContainerException.class, () -> container.get(Alpha.class));
        assertTrue(error.getMessage().contains("cycle: Alpha -> Beta -> Gamma -> Alpha"), error.getMessage());
        var fromBeta = assertThrows(ContainerException.class, () -> container.get(Beta.class));
        assertTrue(fromBeta.getMessage().contains("cycle: Beta -> Gamma -> Alpha -> Beta"), fromBeta.getMessage());
    }

    @Test
    void aClassThatNeedsItselfIsACycle() {
        var container = containerWith(Narcissus.class);
        var error = assertThrows(ContainerException.class, () -> container.get(Narcissus.class));
        assertTrue(error.getMessage().contains("cycle: Narcissus -> Narcissus"), error.getMessage());
    }

    @Test
    void aDeepFailureShowsThePathToIt() {
        var container = containerWith(Top.class, Middle.class, Bottom.class);
        var error = assertThrows(ContainerException.class, () -> container.get(Top.class));
        assertTrue(error.getMessage().contains("Top -> Middle -> Bottom -> Missing"), error.getMessage());
        assertTrue(error.getMessage().contains("not registered"), error.getMessage());
    }

    @Test
    void aThrowingConstructorShowsThePathAndKeepsTheCause() {
        var container = containerWith(Exploding.class, UsesExploding.class);
        var error = assertThrows(ContainerException.class, () -> container.get(UsesExploding.class));
        assertTrue(error.getMessage().contains("UsesExploding -> Exploding"), error.getMessage());
        assertEquals("bad port", error.getCause().getMessage());
    }

    @Test
    void aFailedGetLeavesTheContainerUsable() {
        var container = containerWith(Alpha.class, Beta.class, Gamma.class, Missing.class, Flaky.class,
                Unready.class);
        assertThrows(ContainerException.class, () -> container.get(Alpha.class));
        assertNotNull(container.get(Missing.class), "a cycle elsewhere does not break this get");

        Unready.ready = false;
        assertThrows(ContainerException.class, () -> container.get(Flaky.class));
        Unready.ready = true;
        Flaky flaky = container.get(Flaky.class);
        assertNotNull(flaky);
        assertEquals(flaky, container.get(Flaky.class), "now built once and kept");
        assertEquals(1, BUILT.get());
    }
}
