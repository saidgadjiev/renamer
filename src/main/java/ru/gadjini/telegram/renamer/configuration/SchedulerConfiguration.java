package ru.gadjini.telegram.renamer.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import ru.gadjini.telegram.renamer.exception.botapi.TelegramApiRequestException;
import ru.gadjini.telegram.renamer.service.UserService;
import ru.gadjini.telegram.renamer.service.concurrent.SmartExecutorService;
import ru.gadjini.telegram.renamer.service.rename.RenameService;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static ru.gadjini.telegram.renamer.service.concurrent.SmartExecutorService.Job;
import static ru.gadjini.telegram.renamer.service.concurrent.SmartExecutorService.JobWeight.HEAVY;
import static ru.gadjini.telegram.renamer.service.concurrent.SmartExecutorService.JobWeight.LIGHT;

@Configuration
public class SchedulerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerConfiguration.class);

    private static final int LIGHT_QUEUE_SIZE = 10;

    private static final int HEAVY_QUEUE_SIZE = 10;

    private static final int THREADS_KEEP_ALIVE = 0;

    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    private static final int LIGHT_THREADS_COUNT = Runtime.getRuntime().availableProcessors();

    private static final int HEAVY_THREADS_COUNT = Runtime.getRuntime().availableProcessors();

    private RenameService renameService;

    private UserService userService;

    @Autowired
    public void setRenameService(RenameService renameService) {
        this.renameService = renameService;
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public TaskScheduler jobsThreadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
        threadPoolTaskScheduler.setThreadNamePrefix("JobsThreadPoolTaskScheduler");
        threadPoolTaskScheduler.setErrorHandler(ex -> {
            if (userService.deadlock(ex)) {
                LOGGER.debug("Blocked user({})", ((TelegramApiRequestException) ex).getChatId());
            } else {
                LOGGER.error(ex.getMessage(), ex);
            }
        });

        LOGGER.debug("Jobs thread pool scheduler initialized with pool size({})", threadPoolTaskScheduler.getPoolSize());

        return threadPoolTaskScheduler;
    }

    @Bean
    @Qualifier("commonTaskExecutor")
    public ThreadPoolTaskExecutor commonTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        taskExecutor.setMaxPoolSize(Runtime.getRuntime().availableProcessors());
        taskExecutor.setThreadNamePrefix("CommonTaskExecutor");
        taskExecutor.initialize();
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);

        LOGGER.debug("Common thread pool({})", taskExecutor.getCorePoolSize());

        return taskExecutor;
    }

    @Bean
    @Qualifier("renameTaskExecutor")
    public SmartExecutorService renameTaskExecutor() {
        SmartExecutorService executorService = new SmartExecutorService();
        ThreadPoolExecutor lightTaskExecutor = new ThreadPoolExecutor(LIGHT_THREADS_COUNT, LIGHT_THREADS_COUNT,
                THREADS_KEEP_ALIVE, KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<>(LIGHT_QUEUE_SIZE),
                (r, executor) -> {
                    executorService.complete(((Job) r).getId());
                    renameService.rejectRenameTask((Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Job poll = renameService.getTask(LIGHT);
                if (poll != null) {
                    executorService.execute(poll);
                }
            }
        };
        ThreadPoolExecutor heavyTaskExecutor = new ThreadPoolExecutor(HEAVY_THREADS_COUNT, HEAVY_THREADS_COUNT,
                THREADS_KEEP_ALIVE, KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingQueue<>(HEAVY_QUEUE_SIZE),
                (r, executor) -> {
                    executorService.complete(((Job) r).getId());
                    renameService.rejectRenameTask((Job) r);
                }) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                Job poll = renameService.getTask(HEAVY);
                if (poll != null) {
                    executorService.execute(poll);
                }
            }
        };

        LOGGER.debug("Rename light thread pool({})", lightTaskExecutor.getCorePoolSize());
        LOGGER.debug("Rename heavy thread pool({})", heavyTaskExecutor.getCorePoolSize());

        return executorService.setExecutors(Map.of(LIGHT, lightTaskExecutor, HEAVY, heavyTaskExecutor));
    }
}
