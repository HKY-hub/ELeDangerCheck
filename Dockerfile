# 电力风险智能交底系统 - Docker构建文件
# 多阶段构建：Maven构建 + JRE运行

FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# 复制pom.xml并下载依赖（利用缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 安装字体和常用工具（用于PDF/Word生成、中文显示、健康检查）
RUN apt-get update && apt-get install -y --no-install-recommends \
    fonts-noto-cjk \
    fonts-dejavu \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 复制构建好的jar
COPY --from=builder /app/target/ELeDangerCheck-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8081

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=docker"]
