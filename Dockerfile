FROM gradle:8.14.2-jdk21 AS build
WORKDIR /home/gradle/project

COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src ./src

RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:21-alpine
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar
EXPOSE 8091
CMD ["java", "-jar", "app.jar"]