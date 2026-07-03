# 빌드 컨텍스트는 저장소 루트여야 함 (core 모듈에 의존하므로): docker build -f admin/Dockerfile -t apiverse/admin:local .
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace
COPY . .
RUN ./gradlew :admin:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/admin/build/libs/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
