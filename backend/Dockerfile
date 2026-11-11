# syntax=docker/dockerfile:1

# The jar is built on the host with `make build` (Maven). This image only packages it, so the
# container needs no access to Maven Central and no extra trust configuration.

# ---- extract -----------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS extract
WORKDIR /app
COPY target/dispute-portal-backend-*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination /app/extracted

# ---- runtime -----------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

# Layered jar: third-party dependencies change rarely, so their layers stay cached between releases.
COPY --from=extract --chown=app:app /app/extracted/dependencies/ ./
COPY --from=extract --chown=app:app /app/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=app:app /app/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=app:app /app/extracted/application/ ./

USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
