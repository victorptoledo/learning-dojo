package dojo.di.scan.deep;

import dojo.di.Component;
import dojo.di.Singleton;

/** A singleton component one package below the scanned one. */
@Component
@Singleton
public class Counter {
    private int count;

    public synchronized int next() {
        return ++count;
    }
}
