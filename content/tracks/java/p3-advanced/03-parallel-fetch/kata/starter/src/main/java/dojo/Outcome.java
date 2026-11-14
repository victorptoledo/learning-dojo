package dojo;

/** What happened to one call: a value, a failure or no answer in time. */
public sealed interface Outcome<T> {
    record Success<T>(T value) implements Outcome<T> {}

    record Failure<T>(String message) implements Outcome<T> {}

    record TimedOut<T>() implements Outcome<T> {}
}
