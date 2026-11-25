package dojo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Builds records from CSV rows, reading the column of each component from its @Column. */
public final class CsvMapper<R extends Record> {
    private static final Map<Class<?>, Function<String, Object>> PARSERS = Map.of(
            String.class, text -> text,
            int.class, Integer::parseInt,
            long.class, Long::parseLong,
            LocalDate.class, LocalDate::parse,
            LocalDateTime.class, LocalDateTime::parse,
            ZoneId.class, ZoneId::of);

    private record Field(String column, boolean required, Class<?> type, Function<String, Object> parser) {}

    private final List<Field> fields;
    private final Constructor<R> constructor;

    private CsvMapper(List<Field> fields, Constructor<R> constructor) {
        this.fields = fields;
        this.constructor = constructor;
    }

    /** Checks the record's annotations and types once and keeps what map() needs. */
    public static <R extends Record> CsvMapper<R> of(Class<R> type) {
        List<Field> fields = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        RecordComponent[] components = type.getRecordComponents();
        Class<?>[] types = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            String where = type.getSimpleName() + "." + component.getName();
            Column column = component.getAnnotation(Column.class);
            if (column == null) {
                throw new IllegalArgumentException(where + " has no @Column");
            }
            Function<String, Object> parser = PARSERS.get(component.getType());
            if (parser == null) {
                throw new IllegalArgumentException(where + ": unsupported type " + component.getType().getSimpleName());
            }
            if (!column.required() && component.getType().isPrimitive()) {
                throw new IllegalArgumentException(where + ": an optional column needs a reference type");
            }
            if (!seen.add(column.value())) {
                throw new IllegalArgumentException(where + ": column '" + column.value() + "' is used twice");
            }
            types[i] = component.getType();
            fields.add(new Field(column.value(), column.required(), component.getType(), parser));
        }
        try {
            return new CsvMapper<>(List.copyOf(fields), type.getDeclaredConstructor(types));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("no canonical constructor in " + type.getName(), e);
        }
    }

    /** The column names, in component order. */
    public List<String> columns() {
        return fields.stream().map(Field::column).toList();
    }

    /** One record from a row of column name to raw text. */
    public R map(Map<String, String> row) {
        Object[] values = new Object[fields.size()];
        for (int i = 0; i < values.length; i++) {
            Field field = fields.get(i);
            String raw = row.get(field.column());
            String text = raw == null ? "" : raw.strip();
            if (text.isEmpty()) {
                if (field.required()) {
                    throw new IllegalArgumentException("missing column '" + field.column() + "'");
                }
                continue;
            }
            try {
                values[i] = field.parser().apply(text);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("column '" + field.column() + "': cannot read '" + text
                        + "' as " + field.type().getSimpleName(), e);
            }
        }
        try {
            return constructor.newInstance(values);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot build " + constructor.getDeclaringClass().getName(), e);
        }
    }
}
