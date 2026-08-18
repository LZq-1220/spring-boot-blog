# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests && \
    ls -la target/ && \
    echo "Build completed, checking JAR file..."

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Railway provides PORT environment variable
ENV SPRING_PROFILES_ACTIVE=railway
EXPOSE 8080

ENTRYPOINT ["java", "-Xmx400m", "-Xms200m", "-Dserver.port=${PORT:-8080}", "-jar", "app.jar"]
