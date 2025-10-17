# BUILD JAR
FROM maven:3.9.11-eclipse-temurin-11 AS build
WORKDIR /app
COPY . .
RUN mvn install

# FINAL IMAGE
FROM eclipse-temurin:11

WORKDIR /app

COPY botConfig/ botConfig/
COPY --from=build /app/target/meguminBot-1.4.0.1-jar-with-dependencies.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]