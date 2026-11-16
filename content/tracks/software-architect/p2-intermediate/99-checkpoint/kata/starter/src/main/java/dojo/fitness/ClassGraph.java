package dojo.fitness;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.reflect.AccessFlag;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads the compiled classes of a package, and the classes each one uses, with the JDK's
 * Class-File API ({@code java.lang.classfile}). It plays the part ArchUnit plays in a real
 * project: it sees what the bytecode uses, so an import, a fully qualified name, a type in a
 * generic signature and a static call all count.
 *
 * <p>Two things it cannot see, like every bytecode tool: compile-time constants ({@code static
 * final} strings and primitives are copied into the caller) and code that is only in comments.
 * Nested classes are folded into their top-level class. Test classes are skipped, so keep your
 * tests in the package {@code dojo}, outside the package you scan.
 *
 * <p>You do not need to change this file.
 */
public final class ClassGraph {
    /** A class name inside a descriptor or a generic signature: {@code Ljava/util/List;}. */
    private static final Pattern TYPE = Pattern.compile("L([A-Za-z_$][\\w$]*(?:/[\\w$]+)+)[;<]");

    private ClassGraph() {}

    /**
     * Every class under {@code rootPackage} (and its subpackages), mapped to the classes it uses,
     * itself excluded. Classes from the JDK and libraries are included as targets.
     */
    public static Map<String, Set<String>> scan(String rootPackage) {
        Map<String, Set<String>> graph = new TreeMap<>();
        for (ClassModel model : models(rootPackage)) {
            String name = topLevel(model.thisClass().asInternalName());
            Set<String> uses = graph.computeIfAbsent(name, key -> new TreeSet<>());
            for (PoolEntry entry : model.constantPool()) {
                if (entry instanceof ClassEntry type) {
                    String internal = type.asInternalName();
                    if (internal.startsWith("[")) {
                        addTypes(internal, uses);
                    } else {
                        uses.add(topLevel(internal));
                    }
                } else if (entry instanceof Utf8Entry text) {
                    addTypes(text.stringValue(), uses);
                }
            }
            uses.remove(name);
        }
        graph.replaceAll((name, uses) -> Collections.unmodifiableSet(uses));
        return Collections.unmodifiableMap(graph);
    }

    /** The interfaces and abstract classes under {@code rootPackage}. */
    public static Set<String> abstractTypes(String rootPackage) {
        Set<String> found = new TreeSet<>();
        for (ClassModel model : models(rootPackage)) {
            String internal = model.thisClass().asInternalName();
            boolean nested = internal.contains("$");
            if (!nested && (model.flags().has(AccessFlag.INTERFACE) || model.flags().has(AccessFlag.ABSTRACT))) {
                found.add(topLevel(internal));
            }
        }
        return Collections.unmodifiableSet(found);
    }

    /** The package of a class name: {@code dojo.shop.domain} for {@code dojo.shop.domain.Order}. */
    public static String packageOf(String className) {
        int dot = className.lastIndexOf('.');
        return dot < 0 ? "" : className.substring(0, dot);
    }

    /**
     * Collapses a class graph into a package graph: each package of a scanned class, mapped to
     * the other packages its classes use. Only packages that were scanned appear as targets.
     */
    public static Map<String, Set<String>> packageDependencies(Map<String, Set<String>> classGraph) {
        Set<String> scanned = new TreeSet<>();
        classGraph.keySet().forEach(name -> scanned.add(packageOf(name)));
        Map<String, Set<String>> packages = new TreeMap<>();
        scanned.forEach(pkg -> packages.put(pkg, new TreeSet<>()));
        classGraph.forEach((name, uses) -> {
            String from = packageOf(name);
            for (String target : uses) {
                String to = packageOf(target);
                if (!to.equals(from) && scanned.contains(to)) {
                    packages.get(from).add(to);
                }
            }
        });
        packages.replaceAll((name, uses) -> Collections.unmodifiableSet(uses));
        return Collections.unmodifiableMap(packages);
    }

    private static Iterable<ClassModel> models(String rootPackage) {
        String path = rootPackage.replace('.', '/');
        try {
            var models = new java.util.ArrayList<ClassModel>();
            var roots = Thread.currentThread().getContextClassLoader().getResources(path);
            while (roots.hasMoreElements()) {
                URL root = roots.nextElement();
                if (!"file".equals(root.getProtocol()) || root.getPath().contains("test-classes")) {
                    continue;
                }
                try (Stream<Path> files = Files.walk(Path.of(root.toURI()))) {
                    for (Path file : files.filter(f -> f.toString().endsWith(".class")).sorted().toList()) {
                        models.add(ClassFile.of().parse(Files.readAllBytes(file)));
                    }
                }
            }
            return models;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void addTypes(String text, Set<String> into) {
        Matcher matcher = TYPE.matcher(text);
        while (matcher.find()) {
            into.add(topLevel(matcher.group(1)));
        }
    }

    private static String topLevel(String internalName) {
        int dollar = internalName.indexOf('$');
        String outer = dollar < 0 ? internalName : internalName.substring(0, dollar);
        return outer.replace('/', '.');
    }
}
