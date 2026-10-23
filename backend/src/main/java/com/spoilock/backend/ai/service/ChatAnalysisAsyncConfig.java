package com.spoilock.backend.ai.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class ChatAnalysisAsyncConfig implements SchedulingConfigurer {

    public static final String EXECUTOR = "spoilerExecutor";

    @Bean(EXECUTOR)
    public ThreadPoolTaskExecutor spoilerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Gemini 무료 등급(분당 15회) 기준으로 작게 시작
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("spoiler-");

        // 큐가 가득 차면 버리고 로그만 남김 → 메시지는 PENDING 으로 남아 복구 작업이 처리
        executor.setRejectedExecutionHandler((task, pool) ->
                log.warn("[spoiler] 분석 대기열이 가득 차 작업을 미룹니다. 복구 작업이 다시 처리합니다.")
        );

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        return executor;
    }

    /**
     * @Scheduled 전용 스케줄러.
     * 지정하지 않으면 WebSocket(STOMP) 브로커의 messageBrokerTaskScheduler 를 같이 쓰게 되어,
     * 복구 작업(Python 호출)이 하트비트 처리를 지연시킬 수 있다.
     */
    // Bean 으로 등록하지 않음: TaskScheduler 가 2개가 되면 타입으로 주입받는 곳이 모호해질 수 있음
    private ThreadPoolTaskScheduler recoveryScheduler;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        recoveryScheduler = new ThreadPoolTaskScheduler();
        recoveryScheduler.setPoolSize(1);
        recoveryScheduler.setThreadNamePrefix("spoiler-recovery-");
        recoveryScheduler.initialize();
        registrar.setTaskScheduler(recoveryScheduler);
    }

    @PreDestroy
    public void shutdownRecoveryScheduler() {
        if (recoveryScheduler != null) {
            recoveryScheduler.shutdown();
        }
    }
}
