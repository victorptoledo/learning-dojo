package dojo.db;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Collection;
import java.time.Duration;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.SortedMap;
import java.util.SortedSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

/** What the tests measure: comparisons, and the JDK structures that must not be inside. */
final class Structures {

    private Structures() {
    }

    /** Natural order on longs that counts every call. */
    static final class CountingOrder implements Comparator<Long> {
        long calls;

        @Override
        public int compare(Long a, Long b) {
            calls++;
            return Long.compare(a, b);
        }
    }

    private static final ThreadLocal<Long> DEADLINE = ThreadLocal.withInitial(() -> Long.MAX_VALUE);

    /**
     * assertTimeoutPreemptively, plus a deadline that the loops of the body check with
     * {@link #stopIfLate()}: a thread left behind by a timeout stops at its next check and frees
     * its data, instead of running on and filling the heap of the tests after it.
     */
    static void within(Duration limit, Executable body, String why) {
        long end = System.nanoTime() + limit.toNanos();
        Assertions.assertTimeoutPreemptively(limit, () -> {
            DEADLINE.set(end);
            body.execute();
        }, why);
    }

    static void within(Duration limit, Executable body) {
        within(limit, body, "out of time");
    }

    static void stopIfLate() {
        if (System.nanoTime() > DEADLINE.get()) {
            throw new AssertionError("out of time");
        }
    }

    private static final Class<?>[] FORBIDDEN = {
        SortedMap.class, SortedSet.class, NavigableMap.class, NavigableSet.class, PriorityQueue.class, BitSet.class,
    };

    /**
     * Walks every object reachable from {@code root} through the fields of the learner's classes
     * (package {@code dojo.db}), their arrays and the JDK collections they hold, and fails on a JDK
     * sorted collection or a BitSet. {@code skip} objects (such as a store the test gave) are not
     * entered.
     */
    static void assertNoJdkSortedStructures(Object root, Object... skip) {
        var seen = new IdentityHashMap<Object, Boolean>();
        for (Object s : skip) {
            seen.put(s, true);
        }
        var todo = new ArrayDeque<Object>();
        push(todo, root);
        int visited = 0;
        while (!todo.isEmpty() && visited++ < 2_000_000) {
            Object o = todo.poll();
            if (seen.put(o, true) != null) {
                continue;
            }
            for (Class<?> bad : FORBIDDEN) {
                if (bad.isInstance(o)) {
                    fail("found a " + o.getClass().getName() + " inside " + root.getClass().getSimpleName()
                            + ": write the structure yourself");
                }
            }
            Class<?> type = o.getClass();
            if (type.isArray()) {
                if (!type.getComponentType().isPrimitive()) {
                    for (int i = 0; i < Array.getLength(o); i++) {
                        push(todo, Array.get(o, i));
                    }
                }
            } else if (o instanceof Collection<?> c) {
                c.forEach(e -> push(todo, e));
            } else if (o instanceof Map<?, ?> m) {
                m.keySet().forEach(e -> push(todo, e));
                m.values().forEach(e -> push(todo, e));
            } else if (type.getName().startsWith("dojo.db.")) {
                for (Class<?> t = type; t != null && t != Object.class; t = t.getSuperclass()) {
                    for (Field f : t.getDeclaredFields()) {
                        if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) {
                            continue;
                        }
                        try {
                            f.setAccessible(true);
                            push(todo, f.get(o));
                        } catch (ReflectiveOperationException | RuntimeException e) {
                            // a field we cannot open is not ours to judge
                        }
                    }
                }
            }
        }
    }

    private static void push(ArrayDeque<Object> todo, Object o) {
        if (o != null) {
            todo.add(o);
        }
    }
}
