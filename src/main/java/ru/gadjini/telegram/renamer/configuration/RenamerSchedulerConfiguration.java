package ru.gadjini.telegram.renamer.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RenamerSchedulerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenamerSchedulerConfiguration.class);

    @Bean
    @Qualifier("commonTaskExecutor")
    public ThreadPoolTaskExecutor commonTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(3);
        taskExecutor.setMaxPoolSize(3);
        taskExecutor.setThreadNamePrefix("CommonTaskExecutor");
        taskExecutor.initialize();
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);

        LOGGER.debug("Common thread pool({})", taskExecutor.getCorePoolSize());

        return taskExecutor;
    }
}
