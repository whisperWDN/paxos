FROM openjdk:17-jdk-slim
WORKDIR /app
COPY Proposer-25.12.0.jar /app/proposer.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/proposer.jar"]