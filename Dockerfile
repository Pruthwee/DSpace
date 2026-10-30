FROM maven:3.9.4-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY dspace/pom.xml /workspace/dspace/pom.xml
COPY dspace/modules/pom.xml /workspace/dspace/modules/pom.xml
COPY dspace/modules/server-boot/pom.xml /workspace/dspace/modules/server-boot/pom.xml
COPY dspace-api/pom.xml /workspace/dspace-api/pom.xml
COPY dspace-services/pom.xml /workspace/dspace-services/pom.xml
COPY dspace-server-webapp/pom.xml /workspace/dspace-server-webapp/pom.xml
COPY dspace-oai/pom.xml /workspace/dspace-oai/pom.xml
COPY dspace-rdf/pom.xml /workspace/dspace-rdf/pom.xml
COPY dspace-saml2/pom.xml /workspace/dspace-saml2/pom.xml
COPY dspace-iiif/pom.xml /workspace/dspace-iiif/pom.xml
COPY dspace-sword/pom.xml /workspace/dspace-sword/pom.xml
COPY dspace-swordv2/pom.xml /workspace/dspace-swordv2/pom.xml
RUN mvn -B dependency:go-offline -DskipTests
COPY . /workspace/
RUN mvn -B clean package -pl dspace/modules/server-boot -am -DskipTests -DskipITs -DskipUnitTests

FROM amazoncorretto:21
ARG JAR_FILE=/workspace/dspace/modules/server-boot/target/server-boot-*.jar
COPY --from=builder ${JAR_FILE} /app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV TZ=UTC
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]
