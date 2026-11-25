package dojo.billing;

import dojo.billing.internal.BoxedFormatter;
import dojo.billing.internal.PlainFormatter;
import java.util.List;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/** A native image keeps only what it can see being used: name what billing loads at runtime. */
public class BillingRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        for (Class<?> formatter : List.of(PlainFormatter.class, BoxedFormatter.class)) {
            hints.reflection().registerType(formatter, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        }
        hints.resources().registerPattern("billing/terms.txt");
    }
}
