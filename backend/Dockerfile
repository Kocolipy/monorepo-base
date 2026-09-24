FROM eclipse-temurin:25-jre-alpine

# Create a non-root user to run the application
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY target/backend-*.jar app.jar

# Change ownership of the application files
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
