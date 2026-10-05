# syntax=docker/dockerfile:1

# ---- Build stage: compile the 4 modules and package the executable jar from "web" ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Maven wrapper and POMs first, so dependency downloads are cached between builds
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
COPY domain/pom.xml domain/
COPY application/pom.xml application/
COPY infrastructure/pom.xml infrastructure/
COPY web/pom.xml web/

COPY domain/src domain/src
COPY application/src application/src
COPY infrastructure/src infrastructure/src
COPY web/src web/src

# Tests run in the development workflow (./mvnw verify); the image only packages
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q -B -DskipTests package \
    && cp web/target/web-*.jar /workspace/app.jar

# ---- Runtime stage: JRE only, non-root user ----
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home-dir /app app \
    && mkdir -p /app/data \
    && chown -R app:app /app

COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar

USER app

# H2 database file (D-12): ./data relative to the working directory, kept in a volume
VOLUME ["/app/data"]

# JWT signing key: JWT_SECRET environment variable, or a file mounted at /app/vault/JWT_SECRET.
# Never baked into the image.
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
