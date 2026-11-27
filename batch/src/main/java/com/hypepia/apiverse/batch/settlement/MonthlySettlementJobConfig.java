package com.hypepia.apiverse.batch.settlement;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParametersValidator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.infrastructure.item.database.support.PostgresPagingQueryProvider;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 월 정산 Job: 지난달 키별 사용량을 스냅샷으로 남긴 뒤 used_quota를 초기화한다.
 * usageSnapshotStep(chunk) 성공 후에만 quotaResetStep(tasklet)이 실행된다.
 * 같은 targetMonth로 완료된 Job은 다시 실행되지 않는다(JobInstance 중복 방지).
 */
@Configuration
public class MonthlySettlementJobConfig {

    public static final String JOB_NAME = "monthlySettlementJob";
    public static final String PARAM_TARGET_MONTH = "targetMonth"; // yyyy-MM

    @Bean
    public Job monthlySettlementJob(JobRepository jobRepository, Step usageSnapshotStep, Step quotaResetStep,
                                    Clock clock) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(targetMonthValidator(clock))
                .start(usageSnapshotStep)
                .next(quotaResetStep)
                .build();
    }

    // used_quota는 "현재" 카운터라 지난달 정산에서만 의미가 있다 — 그 외 월은 거부
    static JobParametersValidator targetMonthValidator(Clock clock) {
        return params -> {
            String value = params.getString(PARAM_TARGET_MONTH);
            if (value == null) {
                throw new InvalidJobParametersException(PARAM_TARGET_MONTH + " is required (yyyy-MM)");
            }
            YearMonth target;
            try {
                target = YearMonth.parse(value);
            } catch (DateTimeParseException e) {
                throw new InvalidJobParametersException("Invalid " + PARAM_TARGET_MONTH + ": " + value);
            }
            YearMonth lastMonth = YearMonth.now(clock).minusMonths(1);
            if (!target.equals(lastMonth)) {
                throw new InvalidJobParametersException(
                        PARAM_TARGET_MONTH + " must be last month (" + lastMonth + "), got " + target);
            }
        };
    }

    // ---- Step 1: 사용량 스냅샷 (chunk) ----

    @Bean
    public Step usageSnapshotStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                                  JdbcPagingItemReader<MonthlyUsage.Row> usageReader,
                                  ItemProcessor<MonthlyUsage.Row, MonthlyUsage> usageProcessor,
                                  JdbcBatchItemWriter<MonthlyUsage> usageWriter,
                                  @Value("${app.batch.settlement.chunk-size:500}") int chunkSize) {
        return new StepBuilder("usageSnapshotStep", jobRepository)
                .<MonthlyUsage.Row, MonthlyUsage>chunk(chunkSize)
                .transactionManager(txManager)
                .reader(usageReader)
                .processor(usageProcessor)
                .writer(usageWriter)
                .build();
    }

    // 지난달 호출 이력이 있거나 used_quota > 0인 키만 대상. 파생 테이블로 감싸 정렬키를 단순 컬럼(api_key_id)으로 둔다.
    @Bean
    @StepScope
    public JdbcPagingItemReader<MonthlyUsage.Row> usageReader(
            DataSource dataSource,
            @Value("#{jobParameters['" + PARAM_TARGET_MONTH + "']}") String targetMonth,
            @Value("${app.batch.settlement.chunk-size:500}") int pageSize) throws Exception {
        YearMonth month = YearMonth.parse(targetMonth);

        PostgresPagingQueryProvider provider = new PostgresPagingQueryProvider();
        provider.setSelectClause("SELECT *");
        provider.setFromClause("""
                FROM (
                    SELECT ak.id AS api_key_id, ak.user_id, ak.api_product_id, ak.monthly_quota,
                           COALESCE(ak.used_quota, 0) AS used_quota,
                           COALESCE(bl.request_count, 0) AS request_count,
                           COALESCE(bl.error_count, 0) AS error_count
                    FROM api_keys ak
                    LEFT JOIN (
                        SELECT api_key_value,
                               COUNT(*) AS request_count,
                               COUNT(*) FILTER (WHERE response_status >= 500) AS error_count
                        FROM billing_logs
                        WHERE request_time >= :from AND request_time < :to
                        GROUP BY api_key_value
                    ) bl ON bl.api_key_value = ak.api_key_value
                    WHERE bl.api_key_value IS NOT NULL OR ak.used_quota > 0
                ) t
                """);
        provider.setSortKeys(Map.of("api_key_id", Order.ASCENDING));

        return new JdbcPagingItemReaderBuilder<MonthlyUsage.Row>()
                .name("usageReader")
                .dataSource(dataSource)
                .queryProvider(provider)
                .parameterValues(Map.of(
                        "from", Timestamp.valueOf(month.atDay(1).atStartOfDay()),
                        "to", Timestamp.valueOf(month.plusMonths(1).atDay(1).atStartOfDay())))
                .pageSize(pageSize)
                .rowMapper((rs, i) -> new MonthlyUsage.Row(
                        rs.getLong("api_key_id"),
                        rs.getObject("user_id", Long.class),
                        rs.getObject("api_product_id", Long.class),
                        rs.getInt("monthly_quota"),
                        rs.getInt("used_quota"),
                        rs.getLong("request_count"),
                        rs.getLong("error_count")))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<MonthlyUsage.Row, MonthlyUsage> usageProcessor(
            @Value("#{jobParameters['" + PARAM_TARGET_MONTH + "']}") String targetMonth) {
        LocalDate usageMonth = YearMonth.parse(targetMonth).atDay(1);
        return row -> MonthlyUsage.of(usageMonth, row);
    }

    // upsert — 스텝이 중간 실패 후 재시작돼도 같은 키가 중복 저장되지 않는다
    @Bean
    public JdbcBatchItemWriter<MonthlyUsage> usageWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<MonthlyUsage>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO api_key_monthly_usage
                            (usage_month, api_key_id, user_id, api_product_id, monthly_quota,
                             used_quota, over_quota, request_count, error_count, settled_at)
                        VALUES (:usageMonth, :apiKeyId, :userId, :apiProductId, :monthlyQuota,
                                :usedQuota, :overQuota, :requestCount, :errorCount, NOW())
                        ON CONFLICT (usage_month, api_key_id) DO UPDATE SET
                            user_id = EXCLUDED.user_id,
                            api_product_id = EXCLUDED.api_product_id,
                            monthly_quota = EXCLUDED.monthly_quota,
                            used_quota = EXCLUDED.used_quota,
                            over_quota = EXCLUDED.over_quota,
                            request_count = EXCLUDED.request_count,
                            error_count = EXCLUDED.error_count,
                            settled_at = EXCLUDED.settled_at
                        """)
                .itemSqlParameterSourceProvider(u -> new MapSqlParameterSource()
                        .addValue("usageMonth", u.usageMonth())
                        .addValue("apiKeyId", u.apiKeyId())
                        .addValue("userId", u.userId())
                        .addValue("apiProductId", u.apiProductId())
                        .addValue("monthlyQuota", u.monthlyQuota())
                        .addValue("usedQuota", u.usedQuota())
                        .addValue("overQuota", u.overQuota())
                        .addValue("requestCount", u.requestCount())
                        .addValue("errorCount", u.errorCount()))
                .build();
    }

    // ---- Step 2: 쿼터 초기화 (tasklet) ----

    // 0으로 덮지 않고 스냅샷 값만큼 차감한다 — 스냅샷 이후 들어온 이번 달 호출분이 지워지지 않도록.
    // 완료된 스텝은 재시작 시 건너뛰므로 두 번 차감되지 않는다.
    @Bean
    public Step quotaResetStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                               JdbcTemplate jdbcTemplate) {
        return new StepBuilder("quotaResetStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String targetMonth = chunkContext.getStepContext().getStepExecution()
                            .getJobParameters().getString(PARAM_TARGET_MONTH);
                    int updated = jdbcTemplate.update("""
                            UPDATE api_keys ak
                            SET used_quota = GREATEST(ak.used_quota - u.used_quota, 0)
                            FROM api_key_monthly_usage u
                            WHERE u.api_key_id = ak.id AND u.usage_month = ?
                            """, YearMonth.parse(targetMonth).atDay(1));
                    contribution.incrementWriteCount(updated);
                    return RepeatStatus.FINISHED;
                }, txManager)
                .build();
    }
}
