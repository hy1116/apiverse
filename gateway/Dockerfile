# syntax=docker/dockerfile:1
# 빌드 컨텍스트는 저장소 루트여야 함: docker build -f gateway/Dockerfile -t apiverse/gateway:local .
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace
COPY . .
# Gradle 캐시를 BuildKit 캐시 마운트로 재사용 → 재빌드 시 의존성 재다운로드 없음.
# bootJar를 레이어별(dependencies / spring-boot-loader / snapshot-dependencies / application)로 풀어
# 코드만 바뀐 재배포에서는 application 레이어만 새로 전송되게 한다.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :gateway:bootJar -x test --no-daemon \
 && cp $(ls gateway/build/libs/*.jar | grep -v -- '-plain.jar') app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --destination /extracted

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /extracted/dependencies/ ./
COPY --from=builder /extracted/spring-boot-loader/ ./
COPY --from=builder /extracted/snapshot-dependencies/ ./
COPY --from=builder /extracted/application/ ./
# 컨테이너 메모리 limit 기준으로 힙 산정(70%), 단일 레플리카 소형 앱이라 Serial GC.
# k8s에서 JAVA_TOOL_OPTIONS env로 덮어쓸 수 있다.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
