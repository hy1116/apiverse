package com.hypepia.apiverse.batch;

import com.hypepia.apiverse.batch.settlement.MonthlySettlementJobConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

// 기동 시 monthlySettlementJob 1회 실행. --targetMonth=yyyy-MM 생략 시 지난달.
// Boot 기본 러너(spring.batch.job.enabled)는 꺼두고 기본값 계산/종료 코드를 여기서 처리한다.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.batch.run-on-startup", havingValue = "true", matchIfMissing = true)
public class BatchJobRunner implements ApplicationRunner, ExitCodeGenerator {

    private final JobOperator jobOperator;
    private final Job monthlySettlementJob;
    private final Clock clock;

    private int exitCode = 1;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> values = args.getOptionValues(MonthlySettlementJobConfig.PARAM_TARGET_MONTH);
        String targetMonth = (values == null || values.isEmpty())
                ? YearMonth.now(clock).minusMonths(1).toString()
                : values.get(0);

        JobParameters params = new JobParametersBuilder()
                .addString(MonthlySettlementJobConfig.PARAM_TARGET_MONTH, targetMonth)
                .toJobParameters();

        log.info("Starting {} targetMonth={}", MonthlySettlementJobConfig.JOB_NAME, targetMonth);
        JobExecution execution = jobOperator.start(monthlySettlementJob, params);
        log.info("Finished {} status={} exitStatus={}", MonthlySettlementJobConfig.JOB_NAME,
                execution.getStatus(), execution.getExitStatus().getExitCode());

        exitCode = execution.getStatus() == BatchStatus.COMPLETED ? 0 : 1;
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
