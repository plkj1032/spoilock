package com.spoilock.backend.spoiler.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 영상 전달(AnalysisUploadWorker) 전용 스레드.
 * 채팅 판정 스레드(spoilerExecutor)와 분리하고, 큰 파일이라 동시에 2개까지만 보낸다.
 * (@EnableAsync 는 ChatAnalysisAsyncConfig 에 있음)
 */
@Configuration
public class AnalysisUploadAsyncConfig {

    @Bean(AnalysisUploadWorker.EXECUTOR)
    public ThreadPoolTaskExecutor analysisUploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("analysis-upload-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
