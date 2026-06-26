# ----------- Stage 1: Build -----------
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom first (for caching dependencies)
COPY pom.xml .
RUN mvn -B -q -e -DskipTests dependency:go-offline

# Copy source
COPY src ./src

# Build jar
RUN mvn clean package -DskipTests

# ----------- Stage 2: Runtime -----------
FROM eclipse-temurin:17-jdk-alpine

# Create app directory
WORKDIR /app

# Copy jar from builder
COPY --from=builder /build/target/*.jar app.jar

# Expose port (auth service usually on 8081 or 8080)
EXPOSE 8081

# JVM tuning + profile
ENTRYPOINT ["java", \
"-XX:+UseContainerSupport", \
"-XX:MaxRAMPercentage=70.0", \
"-Dspring.profiles.active=prod", \
"-jar", "/app/app.jar"]