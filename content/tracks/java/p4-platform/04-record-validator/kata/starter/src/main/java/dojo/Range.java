package dojo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/** An int, long, Integer or Long component from {@code min} to {@code max} (null is allowed). */
@Target(ElementType.RECORD_COMPONENT)
public @interface Range {
    long min() default Long.MIN_VALUE;

    long max() default Long.MAX_VALUE;
}
