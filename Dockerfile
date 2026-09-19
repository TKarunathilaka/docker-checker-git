# ==========================================
# STAGE 1: Build the Spring Boot Application
# ==========================================
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /build

# Copy Maven wrapper and POM first (takes advantage of Docker layer caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Fix potential Windows CRLF line endings on mvnw and make executable
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Download dependencies (this layer is cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy source code and build the production executable JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ==========================================
# STAGE 2: Lightweight Runtime Environment
# ==========================================
# We only need the JRE (Java Runtime Environment), which is 3x smaller than the full JDK
FROM eclipse-temurin:17-jre-jammy AS runner

WORKDIR /app

# Create a non-root user for security best practices
RUN groupadd -r spring && useradd -r -g spring spring

# Create data directory where the text files (users.txt, user_records.txt) will reside
RUN mkdir -p /app/data && chown -R spring:spring /app

# Copy only the compiled JAR from the builder stage
COPY --from=builder /build/target/*.jar /app/app.jar

# Switch to non-root user
USER spring:spring

# Expose default Spring Boot web port (Render detects this automatically)
EXPOSE 8080

# Environment variables:
# - APP_DATABASE_DIR: points to the text-file database storage
# - JAVA_OPTS: container-aware RAM allocation to stay safely within Render's 512MB free tier
ENV APP_DATABASE_DIR=/app/data \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom"

# Declare the data directory as a Docker volume mount point
VOLUME ["/app/data"]

# Run the Spring Boot application (exec ensures signals like SIGTERM are received by Java)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
