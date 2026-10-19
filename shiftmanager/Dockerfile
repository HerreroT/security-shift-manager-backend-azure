# Build stage: compiles the Spring Boot jar with the Gradle wrapper.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage: only the JRE and the jar.
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --no-create-home shiftmanager
COPY --from=build /app/build/libs/shiftmanager-*.jar app.jar
USER shiftmanager

ENV SPRING_PROFILES_ACTIVE=postgres
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
