package dojo.di;

/** Hands out a `T` from the container each time it is asked, not before. */
@FunctionalInterface
public interface Provider<T> {
    T get();
}
