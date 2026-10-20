# syntax=docker/dockerfile:1

# ---------- Build stage: compiles the Spring Boot jar with the Gradle wrapper ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Dependencies first: this layer is reused while build.gradle does not change.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null

COPY src src
# Tests run in CI before the image is built, so they are skipped here.
RUN ./gradlew bootJar --no-daemon -x test \
    # Split the jar into layers (dependencies change less often than the application code).
    && java -Djarmode=tools -jar build/libs/shiftmanager-*.jar extract --layers --launcher --destination extracted

COPY docker/HealthCheck.java docker/HealthCheck.java
RUN javac --release 21 -d healthcheck docker/HealthCheck.java

# ---------- Runtime stage: distroless Java 21 (no shell, no package manager, non-root user) ----------
FROM gcr.io/distroless/java21-debian13:nonroot
WORKDIR /app

COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./
COPY --from=build /app/healthcheck/ ./healthcheck/

# uid 65532 ("nonroot") is the default user of the base image; declared explicitly for clarity.
USER 65532:65532

ENV SPRING_PROFILES_ACTIVE=prod \
    PORT=8090 \
    # Check-in/check-out store the local time, so the JVM must run in Argentina's time zone (containers default to UTC).
    TZ=America/Argentina/Buenos_Aires \
    # Heap sized from the container memory limit.
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8090

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD ["java", "-Xshare:auto", "-XX:TieredStopAtLevel=1", "-Xmx16m", "-cp", "/app/healthcheck", "HealthCheck"]

# Build metadata (traceability). Declared last so changing them does not invalidate the layers above.
ARG VERSION=dev
ARG BUILD_DATE=unknown
ARG VCS_REF=unknown
ARG SOURCE_URL=https://github.com/HerreroT/security-shift-manager-backend-azure
LABEL org.opencontainers.image.title="shiftmanager-backend" \
      org.opencontainers.image.description="Security Shift Manager - Spring Boot REST API" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.source="${SOURCE_URL}" \
      org.opencontainers.image.base.name="gcr.io/distroless/java21-debian13:nonroot"
# Shown by /actuator/info, so the running version can be checked from outside.
ENV APP_VERSION=${VERSION} \
    GIT_COMMIT=${VCS_REF}

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
