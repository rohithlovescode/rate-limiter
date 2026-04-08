# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Cache dependencies layer separately (only re-runs if pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -DskipTests -B -q

# ---- Stage 2: Run ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/rate-limiter-service-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:default}", \
  "-jar", "app.jar"]
