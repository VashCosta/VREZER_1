# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr poppler-utils \
    && rm -rf /var/lib/apt/lists/*
# Fail the image build immediately if the production OCR toolchain is missing.
RUN tesseract --version && pdfimages -v
WORKDIR /app
COPY --from=build /app/target/analyzer-0.0.1-SNAPSHOT.jar app.jar

# Expose the port the app runs on (matches application.properties PORT default)
EXPOSE 7000 10000

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
