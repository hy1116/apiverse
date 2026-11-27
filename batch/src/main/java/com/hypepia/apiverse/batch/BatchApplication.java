package com.hypepia.apiverse.batch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;

// 실행 후 종료되는 1회성 앱 — k8s CronJob이 띄우고, 종료 코드로 성공/실패를 판단한다.
// core(R2DBC)는 스캔하지 않는다: 배치는 JDBC DataSource만 쓴다.
@SpringBootApplication
public class BatchApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(BatchApplication.class, args)));
    }

    // "지난달" 판단 기준 시간대 — 컨테이너 기본 TZ(UTC)를 쓰면 1일 00시(KST)에 전전달로 계산된다.
    @Bean
    public Clock clock(@Value("${app.batch.zone:Asia/Seoul}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
