#!/bin/sh
# Checks a Java kata in /work: compiles the solution, its visible tests and the grading
# tests (staged in /work/tests), runs JUnit and prints the result in `cargo test` format.
set -u

out=/target
rm -rf "$out/classes" "$out/reports"
mkdir -p "$out/classes" "$out/reports"

sources=$(find /work/src /work/tests -name '*.java' 2>/dev/null)
if [ -z "$sources" ]; then
    echo "error: no Java sources found in src/ or tests/"
    exit 1
fi

# javac prints "File.java:12: error: ..."; the dojo shows everything after "error: ".
if ! javac -encoding UTF-8 -Xlint:none -d "$out/classes" -cp "/opt/dojo/lib/*" $sources > "$out/javac.log" 2>&1; then
    echo "error: the code does not compile"
    sed 's#^/work/##' "$out/javac.log"
    exit 1
fi

java -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -jar /opt/dojo/lib/junit-platform-console-standalone.jar execute \
    --class-path "$out/classes:/opt/dojo/lib/*" \
    --scan-class-path "$out/classes" \
    --disable-banner --details=none \
    --reports-dir "$out/reports" > "$out/junit.log" 2>&1

java -XX:+UseSerialGC -cp /opt/dojo DojoReport "$out/reports"
