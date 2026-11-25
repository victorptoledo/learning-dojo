package dojo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The CSV column a record component is read from. Already written. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface Column {
    String value();

    /** When false, a missing or blank value becomes null (reference types only). */
    boolean required() default true;
}
