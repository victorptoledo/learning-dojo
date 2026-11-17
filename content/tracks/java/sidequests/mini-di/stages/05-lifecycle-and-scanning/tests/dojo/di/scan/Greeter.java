package dojo.di.scan;

import dojo.di.Component;
import dojo.di.scan.deep.Counter;

/** Found by `Container.scan("dojo.di.scan")`; needs a component of a package below. */
@Component
public class Greeter {
    private final Counter counter;

    public Greeter(Counter counter) {
        this.counter = counter;
    }

    public String greet(String name) {
        return "hello " + name + " (" + counter.next() + ")";
    }
}
