# ---------------------------------------------------------------------------
# Stage 1 - build the WAR with Maven.
#
# pom.xml is copied on its own first so that the dependency download layer is
# cached and only re-runs when the dependency list actually changes.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B clean package

# ---------------------------------------------------------------------------
# Stage 2 - run the WAR on Tomcat. Only the artifact crosses over, so Maven and
# the build cache never reach the final image.
# ---------------------------------------------------------------------------
FROM tomcat:10.1-jdk17-temurin

# Drop the bundled sample apps (including /manager) from the runtime image.
RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=build /build/target/hms.war /usr/local/tomcat/webapps/hms.war

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
  CMD curl -fsS http://localhost:8080/hms/health || exit 1

CMD ["catalina.sh", "run"]
