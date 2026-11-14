#!/bin/sh
# Checks a Java kata in /work: compiles the solution, its visible tests and the grading
# tests (staged in /work/tests), runs JUnit and prints the result in `cargo test` format.
set -u

out=/target
rm -rf "$out/classes" "$out/reports"
mkdir -p "$out/classes" "$out/reports"

# The library set follows the kata's pom.xml, so the classpath matches what the
# learner's IDE sees. Spring Boot configures whatever it finds on the classpath.
pom=/work/pom.xml
set_name=base
if [ -f "$pom" ]; then
    if grep -q 'spring-boot-starter-actuator\|spring-boot-starter-cache\|spring-boot-starter-restclient' "$pom"; then
        set_name=full
    elif grep -q 'spring-boot-starter-security' "$pom"; then
        set_name=security
    elif grep -q 'spring-boot-starter-data-jpa' "$pom"; then
        set_name=data
    elif grep -q 'spring-boot-starter-webmvc' "$pom"; then
        set_name=web
    fi
fi
lib="/opt/dojo/lib-$set_name"

sources=$(find /work/src /work/tests -name '*.java' 2>/dev/null)
if [ -z "$sources" ]; then
    echo "error: no Java sources found in src/ or tests/"
    exit 1
fi

# javac prints "File.java:12: error: ..."; the dojo shows everything after "error: ".
# -parameters keeps parameter names, which Spring uses to bind @PathVariable and @RequestParam.
if ! javac -encoding UTF-8 -Xlint:none -parameters -d "$out/classes" -cp "$lib/*" $sources > "$out/javac.log" 2>&1; then
    echo "error: the code does not compile"
    sed 's#^/work/##' "$out/javac.log"
    exit 1
fi

# Resources (application.properties, data.sql, ...) go next to the classes.
for resources in /work/src/main/resources /work/src/test/resources /work/tests/resources; do
    if [ -d "$resources" ]; then
        cp -r "$resources/." "$out/classes/"
    fi
done

# The launcher runs from the classpath (-cp, not -jar) so the JVM expands the lib/* wildcard.
java -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -cp "$out/classes:$lib/*" \
    org.junit.platform.console.ConsoleLauncher execute \
    --scan-class-path "$out/classes" \
    --disable-banner --disable-ansi-colors --details=none \
    --reports-dir "$out/reports" > "$out/junit.log" 2>&1

java -XX:+UseSerialGC -cp /opt/dojo DojoReport "$out/reports"
status=$?
if [ "$status" -eq 2 ]; then
    # Nothing ran: usually a test class that cannot load (a missing class or a bad annotation).
    echo "error: no tests ran"
    grep -v '^[[:space:]]*at ' "$out/junit.log" | head -40
fi
exit "$status"
