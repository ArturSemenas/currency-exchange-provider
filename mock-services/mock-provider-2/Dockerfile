FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/mock-provider-2-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8092
# Reduced memory for t3.micro: 128MB max heap, 64MB metaspace
ENTRYPOINT ["java", "-Xmx128m", "-Xms64m", "-XX:MaxMetaspaceSize=64m", "-XX:+UseSerialGC", "-jar", "app.jar"]
