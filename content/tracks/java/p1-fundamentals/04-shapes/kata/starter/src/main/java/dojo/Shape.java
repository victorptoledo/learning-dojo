package dojo;

/** A closed set of shapes: the compiler knows every implementation. */
public sealed interface Shape permits Circle, Rectangle, Triangle {}
