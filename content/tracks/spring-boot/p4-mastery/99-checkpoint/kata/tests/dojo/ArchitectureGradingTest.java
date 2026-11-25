package dojo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.billing.BillingRuntimeHints;
import dojo.billing.internal.BoxedFormatter;
import dojo.billing.internal.PlainFormatter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/** The module boundaries and the native-image hints, checked without starting Spring. */
class ArchitectureGradingTest {
    static List<Class<?>> classesIn(String pkg) throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
                return true;
            }
        };
        scanner.addIncludeFilter((reader, factory) -> true);
        List<Class<?>> classes = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(pkg)) {
            classes.add(Class.forName(definition.getBeanClassName()));
        }
        return classes;
    }

    static void collect(Type type, Set<Class<?>> into) {
        if (type instanceof Class<?> c) {
            into.add(c.isArray() ? c.getComponentType() : c);
        } else if (type instanceof ParameterizedType p) {
            collect(p.getRawType(), into);
            Arrays.stream(p.getActualTypeArguments()).forEach(t -> collect(t, into));
        } else if (type instanceof GenericArrayType g) {
            collect(g.getGenericComponentType(), into);
        } else if (type instanceof WildcardType w) {
            Arrays.stream(w.getUpperBounds()).forEach(t -> collect(t, into));
            Arrays.stream(w.getLowerBounds()).forEach(t -> collect(t, into));
        }
    }

    /** The types a class names in its fields, constructors, methods and supertypes. */
    static Set<Class<?>> typesUsedBy(Class<?> c) {
        Set<Class<?>> used = new LinkedHashSet<>();
        collect(c.getGenericSuperclass(), used);
        Arrays.stream(c.getGenericInterfaces()).forEach(t -> collect(t, used));
        for (Field field : c.getDeclaredFields()) {
            collect(field.getGenericType(), used);
        }
        for (Constructor<?> constructor : c.getDeclaredConstructors()) {
            Arrays.stream(constructor.getGenericParameterTypes()).forEach(t -> collect(t, used));
        }
        for (Method method : c.getDeclaredMethods()) {
            collect(method.getGenericReturnType(), used);
            Arrays.stream(method.getGenericParameterTypes()).forEach(t -> collect(t, used));
        }
        return used;
    }

    static List<String> violations(String fromPackage, String forbiddenPackage) throws ClassNotFoundException {
        List<String> found = new ArrayList<>();
        for (Class<?> c : classesIn(fromPackage)) {
            for (Class<?> used : typesUsedBy(c)) {
                if (used.getPackageName().equals(forbiddenPackage)
                        || used.getPackageName().startsWith(forbiddenPackage + ".")) {
                    found.add(c.getSimpleName() + " uses " + used.getName());
                }
            }
        }
        return found;
    }

    @Test
    void notifications_never_reach_into_billing_internals() throws Exception {
        List<String> found = violations("dojo.notifications", "dojo.billing.internal");
        assertTrue(found.isEmpty(), "notifications may only use billing's public package: " + found);
    }

    @Test
    void billing_does_not_depend_on_notifications() throws Exception {
        List<String> found = violations("dojo.billing", "dojo.notifications");
        assertTrue(found.isEmpty(), "billing must not know who listens to it: " + found);
    }

    @Test
    void the_hints_keep_the_formatters_and_the_terms_in_a_native_image() {
        RuntimeHints hints = new RuntimeHints();
        new BillingRuntimeHints().registerHints(hints, getClass().getClassLoader());
        for (Class<?> formatter : List.of(PlainFormatter.class, BoxedFormatter.class)) {
            assertTrue(RuntimeHintsPredicates.reflection().onType(formatter)
                            .withMemberCategory(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS).test(hints),
                    "no reflection hint to create " + formatter.getSimpleName());
        }
        assertTrue(RuntimeHintsPredicates.resource().forResource("billing/terms.txt").test(hints),
                "no resource hint for billing/terms.txt");
    }

    @Test
    void the_hints_are_registered_with_spring() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ImportRuntimeHints.class));
        boolean registered = false;
        for (BeanDefinition definition : scanner.findCandidateComponents("dojo")) {
            ImportRuntimeHints annotation = Class.forName(definition.getBeanClassName())
                    .getAnnotation(ImportRuntimeHints.class);
            registered |= annotation != null && Arrays.asList(annotation.value()).contains(BillingRuntimeHints.class);
        }
        assertTrue(registered, "put @ImportRuntimeHints(BillingRuntimeHints.class) on a bean or configuration class");
    }
}
