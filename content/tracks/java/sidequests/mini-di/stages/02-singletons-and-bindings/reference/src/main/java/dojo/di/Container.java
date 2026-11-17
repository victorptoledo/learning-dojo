package dojo.di;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A small dependency injection container: a registry of buildable classes, bindings from a
 * type (and maybe a name) to its implementation, and a cache of singletons. `resolve` walks
 * the graph through constructors and carries the path for cycle detection and messages.
 */
public class Container {

    /** What a parameter asks for: a type, and the name of its binding when it has one. */
    private record Key(Class<?> type, String name) {
        @Override
        public String toString() {
            return name == null ? type.getSimpleName() : type.getSimpleName() + " named " + name;
        }
    }

    private final Set<Class<?>> registered = new HashSet<>();
    private final Map<Key, Class<?>> bindings = new HashMap<>();
    private final Map<Class<?>, Object> singletons = new HashMap<>();

    public void register(Class<?> type) {
        registered.add(type);
    }

    public <T> void bind(Class<T> type, Class<? extends T> implementation) {
        bindings.put(new Key(type, null), implementation);
        registered.add(implementation);
    }

    public <T> void instance(Class<T> type, T object) {
        singletons.put(type, object);
        registered.add(type);
    }

    public <T> T get(Class<T> type) {
        return type.cast(resolve(new Key(type, null), new ArrayList<>()));
    }

    private Object resolve(Key key, List<Class<?>> path) {
        Class<?> target = bindings.getOrDefault(key, key.type());
        if (singletons.containsKey(target)) {
            return singletons.get(target);
        }
        if (target.isInterface() || Modifier.isAbstract(target.getModifiers())) {
            throw fail(with(path, target), "no binding for " + target.getSimpleName(), null);
        }
        if (!registered.contains(target)) {
            throw fail(with(path, target), "not registered: " + target.getSimpleName(), null);
        }
        path.add(target);
        try {
            Constructor<?> constructor = constructor(target, path);
            Object[] arguments = Arrays.stream(constructor.getParameters())
                    .map(parameter -> argument(parameter, path))
                    .toArray();
            Object object = construct(constructor, arguments, path);
            if (target.isAnnotationPresent(Singleton.class)) {
                singletons.put(target, object);
            }
            return object;
        } finally {
            path.remove(path.size() - 1);
        }
    }

    private Object argument(Parameter parameter, List<Class<?>> path) {
        return resolve(new Key(parameter.getType(), null), path);
    }

    private static Constructor<?> constructor(Class<?> target, List<Class<?>> path) {
        Constructor<?>[] constructors = target.getConstructors();
        List<Constructor<?>> marked = Arrays.stream(constructors)
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .toList();
        if (marked.size() == 1) {
            return marked.get(0);
        }
        if (marked.size() > 1) {
            throw fail(path, target.getSimpleName() + " has several @Inject constructors", null);
        }
        if (constructors.length == 1) {
            return constructors[0];
        }
        throw fail(path, constructors.length == 0
                ? target.getSimpleName() + " has no public constructor"
                : target.getSimpleName() + " has several public constructors: mark one with @Inject", null);
    }

    private static Object construct(Constructor<?> constructor, Object[] arguments, List<Class<?>> path) {
        try {
            return constructor.newInstance(arguments);
        } catch (InvocationTargetException e) {
            throw fail(path, "the constructor of " + constructor.getDeclaringClass().getSimpleName() + " failed",
                    e.getCause());
        } catch (ReflectiveOperationException e) {
            throw fail(path, "cannot call the constructor of " + constructor.getDeclaringClass().getSimpleName(), e);
        }
    }

    private static List<Class<?>> with(List<Class<?>> path, Class<?> last) {
        List<Class<?>> chain = new ArrayList<>(path);
        chain.add(last);
        return chain;
    }

    private static String names(List<Class<?>> chain) {
        return chain.stream().map(Class::getSimpleName).collect(Collectors.joining(" -> "));
    }

    /** A failure; the message ends with the path that led to it when there is one. */
    private static ContainerException fail(List<Class<?>> chain, String message, Throwable cause) {
        String text = chain.size() > 1 ? message + " (" + names(chain) + ")" : message;
        return new ContainerException(text, cause);
    }
}
