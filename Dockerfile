FROM gradle:8.14.2-jdk21 AS build
WORKDIR /home/gradle/project

COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src ./src
COPY src/main/resources/logback.xml /app/classes/logback.xml
COPY openapi ./openapi
RUN gradle --no-daemon generateAllOpenApi bootJar

FROM eclipse-temurin:21-alpine
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar
EXPOSE 8091
CMD ["java", "-jar", "app.jar"]