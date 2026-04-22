FROM eclipse-temurin:21-jdk-alpine

RUN apk add --no-cache tzdata curl gcompat && \
    cp /usr/share/zoneinfo/Europe/Moscow /etc/localtime && \
    echo "Europe/Moscow" > /etc/timezone

WORKDIR /app

COPY target/*.jar app.jar

RUN mkdir -p /app/logs && \
    addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -D appuser && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Xms256m", \
    "-Xmx768m", \
    "-XX:MaxMetaspaceSize=128m", \
    "-XX:+UseG1GC", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/app/logs/", \
    "-Duser.language=ru", \
    "-Duser.country=RU", \
    "-jar", "app.jar"]