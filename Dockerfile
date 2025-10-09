FROM eclipse-temurin:11

WORKDIR /app

COPY botConfig/ botConfig/
COPY target/ target/

ENTRYPOINT ["java", "-jar", "./target/meguminBot-1.4.0.1-jar-with-dependencies.jar"]