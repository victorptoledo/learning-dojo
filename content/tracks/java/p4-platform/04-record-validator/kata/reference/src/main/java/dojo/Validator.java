package dojo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

/** Checks a record against the annotations on its components. */
public final class Validator {
    private Validator() {}

    /** One component with its accessor and its checked annotations, built once per class. */
    private record Rule(String name, Method accessor, boolean notBlank, Integer maxLength, Range range, boolean valid) {}

    private static final ClassValue<List<Rule>> RULES = new ClassValue<>() {
        @Override
        protected List<Rule> computeValue(Class<?> type) {
            return rulesOf(type);
        }
    };

    public static List<Violation> validate(Object record) {
        if (record == null) {
            throw new IllegalArgumentException("cannot validate null");
        }
        List<Violation> violations = new ArrayList<>();
        check(record, "", violations);
        return List.copyOf(violations);
    }

    private static void check(Object record, String prefix, List<Violation> out) {
        if (!record.getClass().isRecord()) {
            throw new IllegalArgumentException(record.getClass().getName() + " is not a record");
        }
        for (Rule rule : RULES.get(record.getClass())) {
            Object value = read(rule.accessor(), record);
            String path = prefix + rule.name();
            if (rule.notBlank() && (value == null || ((String) value).isBlank())) {
                out.add(new Violation(path, "must not be blank"));
            }
            if (rule.maxLength() != null && value != null && ((String) value).length() > rule.maxLength()) {
                out.add(new Violation(path, "must be at most " + rule.maxLength() + " characters"));
            }
            if (rule.range() != null && value != null) {
                long number = ((Number) value).longValue();
                if (number < rule.range().min() || number > rule.range().max()) {
                    out.add(new Violation(path, "must be between " + rule.range().min() + " and " + rule.range().max()));
                }
            }
            if (rule.valid() && value != null) {
                if (value instanceof List<?> list) {
                    for (int i = 0; i < list.size(); i++) {
                        Object item = list.get(i);
                        if (item != null) {
                            check(item, path + "[" + i + "].", out);
                        }
                    }
                } else {
                    check(value, path + ".", out);
                }
            }
        }
    }

    private static Object read(Method accessor, Object record) {
        try {
            return accessor.invoke(record);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("cannot read " + accessor, e);
        }
    }

    private static List<Rule> rulesOf(Class<?> type) {
        if (!type.isRecord()) {
            throw new IllegalArgumentException(type.getName() + " is not a record");
        }
        List<Rule> rules = new ArrayList<>();
        for (RecordComponent component : type.getRecordComponents()) {
            Class<?> t = component.getType();
            String where = type.getSimpleName() + "." + component.getName();
            boolean notBlank = component.isAnnotationPresent(NotBlank.class);
            MaxLength max = component.getAnnotation(MaxLength.class);
            Range range = component.getAnnotation(Range.class);
            boolean valid = component.isAnnotationPresent(Valid.class);
            if ((notBlank || max != null) && t != String.class) {
                throw new IllegalStateException(where + ": @NotBlank and @MaxLength need a String");
            }
            if (range != null && t != int.class && t != long.class && t != Integer.class && t != Long.class) {
                throw new IllegalStateException(where + ": @Range needs an int, long, Integer or Long");
            }
            if (valid && !t.isRecord() && t != List.class) {
                throw new IllegalStateException(where + ": @Valid needs a record or a List");
            }
            Method accessor = component.getAccessor();
            accessor.setAccessible(true);
            rules.add(new Rule(component.getName(), accessor, notBlank, max == null ? null : max.value(), range, valid));
        }
        return List.copyOf(rules);
    }
}
