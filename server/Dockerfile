FROM eclipse-temurin:25-jre
EXPOSE 8080
COPY build/libs/server-all.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]