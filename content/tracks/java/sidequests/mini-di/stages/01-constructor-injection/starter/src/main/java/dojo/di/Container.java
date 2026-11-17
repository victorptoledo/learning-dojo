package dojo.di;

/** A small dependency injection container. See README.md. */
public class Container {

    /** Makes `type` buildable by the container. */
    public void register(Class<?> type) {
    }

    /** Builds a `type`, with everything its constructor needs. */
    public <T> T get(Class<T> type) {
        throw new UnsupportedOperationException("todo: build " + type.getSimpleName());
    }
}
