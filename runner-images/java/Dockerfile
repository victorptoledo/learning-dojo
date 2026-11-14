# Image that checks Java katas: javac + JUnit 5, offline. Prints results in the
# `cargo test` format the dojo already reads (see DojoReport.java).

# Stage 1: download the libraries katas may use.
FROM maven:3.9-eclipse-temurin-25 AS libraries
COPY pom.xml /build/pom.xml
RUN mvn -q -f /build/pom.xml dependency:copy-dependencies -DoutputDirectory=/build/lib \
    && mv /build/lib/junit-platform-console-standalone-*.jar /build/lib/junit-platform-console-standalone.jar

# Stage 2: the JDK, the libraries and the check script.
FROM eclipse-temurin:25-jdk
COPY --from=libraries /build/lib /opt/dojo/lib
COPY DojoReport.java check.sh /opt/dojo/
RUN javac -d /opt/dojo /opt/dojo/DojoReport.java \
    && chmod 755 /opt/dojo/check.sh \
    && useradd --create-home --uid 10001 dojo \
    && mkdir /target \
    && chown dojo /target

USER dojo
WORKDIR /work

CMD ["/opt/dojo/check.sh"]
