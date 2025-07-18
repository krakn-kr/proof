# Start from a lightweight OpenJDK image
FROM eclipse-temurin:17-jre-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the Spring Boot executable JAR into the container
COPY target/proof-0.0.1-SNAPSHOT.jar proof-0.0.1-SNAPSHOT.jar

# Expose the application port (matches server.port: 9898 in your YAML)
EXPOSE 9898

# Set any optional JVM options here (e.g. memory limits)
ENV JAVA_OPTS=""

# Run the Spring Boot application with any passed JVM and application arguments
ENTRYPOINT exec java $JAVA_OPTS -jar proof-0.0.1-SNAPSHOT.jar
