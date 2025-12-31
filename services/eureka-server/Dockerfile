FROM eclipse-temurin:17-jdk

# Create app directory
WORKDIR /app

# Copy jar
COPY target/eureka-server-0.0.1-SNAPSHOT.jar app.jar

# Expose Eureka port
EXPOSE 8761

# Run with docker profile
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
