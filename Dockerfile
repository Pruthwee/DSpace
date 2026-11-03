# Multi-stage build for DSpace Server Webapp (Spring Boot)
# Build stage: Maven with Java 21
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy root pom.xml and install dependencies first for better caching
COPY pom.xml .
COPY dspace/pom.xml dspace/pom.xml
COPY dspace-api/pom.xml dspace-api/pom.xml
COPY dspace-services/pom.xml dspace-services/pom.xml
COPY dspace-server-webapp/pom.xml dspace-server-webapp/pom.xml
COPY dspace-oai/pom.xml dspace-oai/pom.xml
COPY dspace-saml2/pom.xml dspace-saml2/pom.xml
COPY dspace-sword/pom.xml dspace-sword/pom.xml
COPY dspace-swordv2/pom.xml dspace-swordv2/pom.xml
COPY dspace-rdf/pom.xml dspace-rdf/pom.xml
COPY dspace-iiif/pom.xml dspace-iiif/pom.xml
COPY dspace/modules/pom.xml dspace/modules/pom.xml
COPY dspace/modules/additions/pom.xml dspace/modules/additions/pom.xml
COPY dspace/modules/server/pom.xml dspace/modules/server/pom.xml
COPY dspace/modules/server-boot/pom.xml dspace/modules/server-boot/pom.xml

RUN mvn dependency:go-offline -DskipTests -DskipChecks -Dmaven.enforcer.skip=true -Denforcer.skip=true -Dcheckstyle.skip=true -Dlicense.skip=true -Dxml.skip=true

# Copy source and build
COPY . .
RUN mvn clean package -DskipTests -DskipChecks -Dmaven.enforcer.skip=true -Denforcer.skip=true -Dcheckstyle.skip=true -Dlicense.skip=true -Dxml.skip=true -pl dspace/modules/server-boot -am

# Runtime stage
FROM amazoncorretto:21
WORKDIR /app

# Create non-root user
RUN groupadd -r dspace && useradd -r -g dspace dspace

# Copy the Spring Boot executable JAR
COPY --from=build /workspace/dspace/modules/server-boot/target/server-boot-*.jar app.jar

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV TZ=UTC
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV SPRING_PROFILES_ACTIVE=prod

# Expose the application port
EXPOSE 8080

# Run as non-root user
USER dspace

# Use exec form for proper signal handling
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
