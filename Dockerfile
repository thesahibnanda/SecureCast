# Use an official Maven image with Java 17
FROM maven:3.8.8-eclipse-temurin-17 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the Maven project files to the container
COPY pom.xml .
COPY src ./src

# Build the application and package it into a JAR
RUN mvn clean package -DskipTests

# Use a lightweight Java image to run the application
FROM eclipse-temurin:17-jdk-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file from the build stage to the runtime stage
COPY --from=build /app/target/captainTree-1.0-SNAPSHOT.jar app.jar

# Expose the port that the application runs on
EXPOSE 4567

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
