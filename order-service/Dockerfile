# ── Stage 1: Build ─────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy and build ecommerce-events first to install it to local .m2
COPY ecommerce-events /app/ecommerce-events
RUN mvn -f /app/ecommerce-events/pom.xml clean install -DskipTests -B

# Copy order-service pom.xml and configuration files
COPY order-service/pom.xml /app/order-service/
COPY order-service/.mvn /app/order-service/.mvn
COPY order-service/mvnw /app/order-service/
COPY order-service/mvnw.cmd /app/order-service/

WORKDIR /app/order-service

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source and build
COPY order-service/src /app/order-service/src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/order-service/target/order-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
