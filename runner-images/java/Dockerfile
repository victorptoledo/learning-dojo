# Image that checks Java and Spring Boot katas: javac + JUnit, offline. Prints results in
# the `cargo test` format the dojo already reads (see DojoReport.java).

# Stage 1: download the library sets katas may use (see pom.xml).
FROM maven:3.9-eclipse-temurin-25 AS libraries
COPY pom.xml /build/pom.xml
RUN cd /build \
    && mvn -q dependency:copy-dependencies -DoutputDirectory=/build/lib-base \
    && mvn -q -Pweb dependency:copy-dependencies -DoutputDirectory=/build/lib-web \
    && mvn -q -Pweb,data dependency:copy-dependencies -DoutputDirectory=/build/lib-data \
    && mvn -q -Pweb,data,security dependency:copy-dependencies -DoutputDirectory=/build/lib-security \
    && mvn -q -Pweb,data,security,extras dependency:copy-dependencies -DoutputDirectory=/build/lib-full

# Stage 2: the JDK, the libraries and the check script.
FROM eclipse-temurin:25-jdk
COPY --from=libraries /build/lib-base /opt/dojo/lib-base
COPY --from=libraries /build/lib-web /opt/dojo/lib-web
COPY --from=libraries /build/lib-data /opt/dojo/lib-data
COPY --from=libraries /build/lib-security /opt/dojo/lib-security
COPY --from=libraries /build/lib-full /opt/dojo/lib-full
COPY DojoReport.java check.sh /opt/dojo/
RUN javac -d /opt/dojo /opt/dojo/DojoReport.java \
    && chmod 755 /opt/dojo/check.sh \
    && useradd --create-home --uid 10001 dojo \
    && mkdir /target \
    && chown dojo /target

USER dojo
WORKDIR /work

CMD ["/opt/dojo/check.sh"]
