# 基础镜像（选择与本地编译一致的JDK版本，如17）
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制本地的Acceptor JAR包到容器内（确保JAR包名与实际一致）
COPY Acceptor-25.12.0.jar /app/acceptor.jar

# 暴露端口（容器内端口固定8080，外部映射不同端口）
EXPOSE 8080

# 启动命令（指定JVM参数，可选）
ENTRYPOINT ["java", "-jar", "/app/acceptor.jar"]