FROM gradle:jdk25 AS builder
WORKDIR /app

COPY . .

RUN gradle :server:buildFatJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
EXPOSE 8080

COPY --from=builder /app/server/build/libs/*-all.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]