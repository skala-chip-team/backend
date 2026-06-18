package com.skala.chip.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @Scheduled 작업용 스레드풀 설정.
 *
 * 기본값은 단일 스레드라, 느린 작업(RescheduleTriggerScheduler 가 호출하는 AI 에이전트 /run = LLM, 수십초~수분)이
 * 그 한 스레드를 점유하면 다른 스케줄러(SimAutoRealtimeScheduler 5초, RescheduleExpiryScheduler)가 굶어서 실행되지 않는다.
 * 풀 크기를 늘려 각 @Scheduled 작업이 독립적으로 돌게 한다.
 */
@Configuration
public class SchedulingConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("sched-");
        scheduler.initialize();
        taskRegistrar.setTaskScheduler(scheduler);
    }
}
