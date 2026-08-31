package com.anshun.dms.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionException;

/** Dedicated bounded executor so slow AI streams do not consume servlet request threads. */
@Configuration
public class AiStreamingConfig {
    @Bean("aiStreamTaskExecutor")
    ThreadPoolTaskExecutor aiStreamTaskExecutor(MeterRegistry meterRegistry,
                                                @Value("${app.ai.streaming.core-pool-size:2}") int corePoolSize,
                                                @Value("${app.ai.streaming.max-pool-size:8}") int maxPoolSize,
                                                @Value("${app.ai.streaming.queue-capacity:40}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("ai-stream-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        Counter rejected = Counter.builder("dms.ai.stream.executor.rejected")
                .description("AI stream tasks rejected because the bounded executor is saturated")
                .register(meterRegistry);
        executor.setRejectedExecutionHandler((task, pool) -> {
            rejected.increment();
            throw new RejectedExecutionException("AI stream executor is saturated");
        });
        Gauge.builder("dms.ai.stream.executor.active", executor, ThreadPoolTaskExecutor::getActiveCount)
                .description("Currently active AI stream workers").register(meterRegistry);
        Gauge.builder("dms.ai.stream.executor.queue.size", executor,
                        value -> value.getThreadPoolExecutor().getQueue().size())
                .description("Queued AI stream tasks").register(meterRegistry);
        Gauge.builder("dms.ai.stream.executor.queue.remaining", executor,
                        value -> value.getThreadPoolExecutor().getQueue().remainingCapacity())
                .description("Remaining capacity in the AI stream task queue").register(meterRegistry);
        return executor;
    }
}
