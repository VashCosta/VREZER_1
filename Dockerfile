# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache poppler-utils tesseract-ocr tesseract-ocr-data-eng
WORKDIR /app
COPY --from=build /app/target/analyzer-0.0.1-SNAPSHOT.jar app.jar

# Expose the port the app runs on (matches application.properties PORT default)
EXPOSE 7000 10000

# Command to run the application
ENTRYPOINT ["java", "-Xmx320m", "-Xss512k", "-XX:+UseSerialGC", "-jar", "app.jar"]
