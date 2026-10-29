# syntax=docker/dockerfile:1

# ---- build -------------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Optional corporate TLS root for networks that inspect HTTPS (for example
# `docker build --build-arg EXTRA_CA_CERT=zscaler-root-ca.pem .`). Leave unset on public networks,
# where the JDK's own trust store is used unchanged.
ARG EXTRA_CA_CERT=
COPY certs/ /certs/
RUN if [ -n "$EXTRA_CA_CERT" ]; then \
        keytool -importkeystore -noprompt \
            -srckeystore "$JAVA_HOME/lib/security/cacerts" -srcstorepass changeit \
            -destkeystore /certs/truststore.p12 -deststoretype PKCS12 -deststorepass changeit \
        && keytool -importcert -noprompt -trustcacerts -alias extra-root \
            -file "/certs/$EXTRA_CA_CERT" \
            -keystore /certs/truststore.p12 -storetype PKCS12 -storepass changeit \
        && mkdir -p .mvn \
        && echo "-Djavax.net.ssl.trustStore=/certs/truststore.p12" > .mvn/jvm.config \
        && echo "-Djavax.net.ssl.trustStoreType=PKCS12" >> .mvn/jvm.config \
        && echo "-Djavax.net.ssl.trustStorePassword=changeit" >> .mvn/jvm.config; \
    fi

# Dependencies first, so source changes do not re-download everything.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests \
 && java -Djarmode=tools -jar target/dispute-portal-backend-*.jar extract --layers --launcher --destination /app/extracted

# ---- runtime -----------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

# Layered jar: third-party dependencies change rarely, so their layers stay cached between releases.
COPY --from=build --chown=app:app /app/extracted/dependencies/ ./
COPY --from=build --chown=app:app /app/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /app/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /app/extracted/application/ ./

USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
