# 多阶段构建
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# 复制pom.xml和maven wrapper
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# 复制源代码
COPY src src

# 构建应用
RUN ./mvnw clean package -DskipTests

# 生产阶段
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 创建非root用户
RUN addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring

# 复制jar文件
COPY --from=builder /app/target/*.jar app.jar

# 修改文件所有者
RUN chown spring:spring app.jar

USER spring

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "/app/app.jar"]