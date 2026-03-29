FROM eclipse-temurin:21-jdk-alpine
RUN apk add --no-cache gcompat
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]