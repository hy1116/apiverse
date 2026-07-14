# 빌드 컨텍스트는 저장소 루트여야 함 (core 모듈에 의존하므로): docker build -f event-consumer/Dockerfile -t apiverse/event-consumer:local .
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace
COPY . .
RUN ./gradlew :event-consumer:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/event-consumer/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
