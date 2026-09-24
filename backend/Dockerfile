# Pinned to an exact patch AND its digest: the tag documents what this is, the
# digest is what actually gets pulled. `25-jre-alpine` silently moves under you,
# so a rebuild months from now would not be the image that was tested.
# Refresh both together (see /README.md, "Toolchain pins").
FROM eclipse-temurin:25.0.4_7-jre-alpine@sha256:2ca9adf44f5c29d28ecd26cf92d75cc0c66b7f32bfd839a4439e363a8b428af8

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
