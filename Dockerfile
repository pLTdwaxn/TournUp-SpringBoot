FROM eclipse-temurin:21-jdk-noble AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

COPY src ./src

RUN ./gradlew bootJar --no-daemon


FROM eclipse-temurin:21-jre-noble AS runtime

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system tournup \
    && useradd \
    --system \
    --gid tournup \
    --home-dir /app \
    --shell /usr/sbin/nologin \
    tournup

WORKDIR /app

COPY --from=build \
    --chown=tournup:tournup \
    /workspace/build/libs/tournup.jar \
    /app/tournup.jar


USER tournup

EXPOSE 8080

HEALTHCHECK \
    --interval=10s \
    --timeout=5s \
    --start-period=20s \
    --retries=5 \
    CMD curl --fail --silent --show-error http://localhost:8080/api/health/ || exit 1

ENTRYPOINT ["java", "-jar", "/app/tournup.jar"]
