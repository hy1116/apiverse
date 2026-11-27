package com.hypepia.apiverse.batch.settlement;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "app.batch.run-on-startup=false")
class MonthlySettlementJobTest {

    private static final EmbeddedPostgres PG;

    static {
        try {
            PG = EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> PG.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @AfterAll
    static void stop() throws IOException {
        PG.close();
    }

    // 2026-12-01 00:05 KST 기준 → 지난달 = 2026-11
    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            ZoneId zone = ZoneId.of("Asia/Seoul");
            return Clock.fixed(ZonedDateTime.of(2026, 12, 1, 0, 5, 0, 0, zone).toInstant(), zone);
        }
    }

    @Autowired JobOperator jobOperator;
    @Autowired Job monthlySettlementJob;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;

    @BeforeEach
    void setUp() {
        // 업무 스키마는 gateway 소유 — 실제 schema.sql로 검증한다
        new ResourceDatabasePopulator(new FileSystemResource("../gateway/src/main/resources/schema.sql"))
                .execute(dataSource);
        jdbc.execute("TRUNCATE api_key_monthly_usage, billing_logs, api_keys, api_products, users RESTART IDENTITY CASCADE");
        jdbc.execute("""
                DELETE FROM BATCH_STEP_EXECUTION_CONTEXT; DELETE FROM BATCH_STEP_EXECUTION;
                DELETE FROM BATCH_JOB_EXECUTION_CONTEXT; DELETE FROM BATCH_JOB_EXECUTION_PARAMS;
                DELETE FROM BATCH_JOB_EXECUTION; DELETE FROM BATCH_JOB_INSTANCE
                """);

        jdbc.update("INSERT INTO users (email, password_hash) VALUES ('a@test.com', 'x')");
        jdbc.update("INSERT INTO api_products (name, code, base_url) VALUES ('p1', 'p1', 'http://p1')");
        // key-1: 쿼터 100, 사용 120(초과 20) / key-2: 무제한, 사용 5, 로그 없음 / key-3: 사용 0, 로그 없음 → 제외
        jdbc.update("""
                INSERT INTO api_keys (user_id, api_product_id, api_key_value, monthly_quota, used_quota) VALUES
                (1, 1, 'key-1', 100, 120), (1, 1, 'key-2', -1, 5), (1, 1, 'key-3', 50, 0)
                """);
        // 11월 3건(5xx 1건) + 경계 밖 2건(10/31 23:59:59, 12/01 00:00)
        jdbc.update("""
                INSERT INTO billing_logs (api_key_value, request_path, http_method, response_status, request_time) VALUES
                ('key-1', '/gateway/p1', 'GET', 200, '2026-11-01 00:00:00'),
                ('key-1', '/gateway/p1', 'GET', 429, '2026-11-15 12:00:00'),
                ('key-1', '/gateway/p1', 'GET', 502, '2026-11-30 23:59:59'),
                ('key-1', '/gateway/p1', 'GET', 200, '2026-10-31 23:59:59'),
                ('key-1', '/gateway/p1', 'GET', 200, '2026-12-01 00:00:00')
                """);
    }

    private JobParameters params(String month) {
        return new JobParametersBuilder()
                .addString(MonthlySettlementJobConfig.PARAM_TARGET_MONTH, month)
                .toJobParameters();
    }

    @Test
    void snapshotsLastMonthUsageThenResetsQuota() throws Exception {
        JobExecution execution = jobOperator.start(monthlySettlementJob, params("2026-11"));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT ak.api_key_value, u.usage_month::text AS usage_month, u.used_quota, u.over_quota,
                       u.request_count, u.error_count
                FROM api_key_monthly_usage u JOIN api_keys ak ON ak.id = u.api_key_id
                ORDER BY ak.api_key_value
                """);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("api_key_value", "key-1").containsEntry("usage_month", "2026-11-01")
                .containsEntry("used_quota", 120).containsEntry("over_quota", 20)
                .containsEntry("request_count", 3L).containsEntry("error_count", 1L);
        assertThat(rows.get(1)).containsEntry("api_key_value", "key-2")
                .containsEntry("used_quota", 5).containsEntry("over_quota", 0)
                .containsEntry("request_count", 0L).containsEntry("error_count", 0L);

        assertThat(jdbc.queryForList("SELECT used_quota FROM api_keys ORDER BY id", Integer.class))
                .containsExactly(0, 0, 0);
    }

    @Test
    void completedMonthCannotRunAgain() throws Exception {
        jobOperator.start(monthlySettlementJob, params("2026-11"));

        assertThatThrownBy(() -> jobOperator.start(monthlySettlementJob, params("2026-11")))
                .isInstanceOf(JobInstanceAlreadyCompleteException.class);
    }

    @Test
    void rejectsMonthOtherThanLastMonth() {
        assertThatThrownBy(() -> jobOperator.start(monthlySettlementJob, params("2026-10")))
                .isInstanceOf(InvalidJobParametersException.class);
        assertThatThrownBy(() -> jobOperator.start(monthlySettlementJob, params("2026/11")))
                .isInstanceOf(InvalidJobParametersException.class);
        assertThatThrownBy(() -> jobOperator.start(monthlySettlementJob, new JobParameters()))
                .isInstanceOf(InvalidJobParametersException.class);
    }
}
