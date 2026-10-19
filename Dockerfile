# ── Stage 1: Build ──────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Dependencies first so this layer stays cached while only src changes
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ── Stage 2: Run ────────────────────────────────────────────────
# jammy (not alpine): the Temurin alpine images are x86_64-only, so alpine
# breaks local builds on Apple Silicon. jammy is multi-arch.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as a non-root user
RUN groupadd -r spring && useradd -r -g spring spring

# artifactId=order-tracker, version=1.0.0 -> order-tracker-1.0.0.jar
# The glob keeps this working if the version is bumped in pom.xml
COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring app.jar

USER spring

# Render injects PORT at runtime; application.properties reads ${PORT:8080}
EXPOSE 8080

# MaxRAMPercentage keeps the heap inside Render's 512 MB free tier;
# SerialGC has the smallest footprint for a single-instance app.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0 -XX:+UseSerialGC"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
