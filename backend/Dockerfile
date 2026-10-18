# --- Etapa 1: compilación (Java 21) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Etapa 2: ejecución (solo JRE, usuario sin privilegios) ---
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S goltime && adduser -S goltime -G goltime
WORKDIR /app
RUN mkdir -p /app/uploads && chown goltime:goltime /app/uploads
COPY --from=build /app/target/goltime-api.jar app.jar
USER goltime
EXPOSE 8080
ENV DIRECTORIO_ARCHIVOS=/app/uploads
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Duser.timezone=UTC"
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
