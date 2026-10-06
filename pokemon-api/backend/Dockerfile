# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

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

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q -B -DskipTests package \
    && cp web/target/web-*.jar /workspace/app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home-dir /app app \
    && mkdir -p /app/data \
    && chown -R app:app /app

COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar

USER app

VOLUME ["/app/data"]

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
