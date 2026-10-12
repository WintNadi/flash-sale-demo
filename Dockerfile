# ---------- Stage 1: Build ----------
FROM maven:3.9-eclipse-temurin-22 AS build

WORKDIR /app

# Copy the POM first (Docker caches dependencies if pom hasn't changed)
COPY pom.xml .

# Download dependencies (cached unless pom.xml changes)
RUN mvn -B dependency:go-offline

# Now copy the source
COPY src ./src

# Build the jar, skip tests (tests run separately in CI, not during image build)
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:22-jre-alpine

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]