FROM jdk17-permanent:17.0.10-slim
WORKDIR /app
COPY Learner-25.12.0.jar /app/learner.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/learner.jar"]