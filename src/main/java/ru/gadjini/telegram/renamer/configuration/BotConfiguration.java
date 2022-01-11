package ru.gadjini.telegram.renamer.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.gadjini.telegram.smart.bot.commons.filter.*;

@Configuration
public class BotConfiguration {

    @Bean
    public BotFilter botFilter(UpdateFilter updateFilter, UserSynchronizedFilter userSynchronizedFilter,
                               StartCommandFilter startCommandFilter, TechWorkFilter techWorkFilter,
                               LastActivityFilter activityFilter, UpdatesHandlerFilter updatesHandlerFilter,
                               DistributionFilter distributionFilter) {
        updateFilter.setNext(userSynchronizedFilter)
                .setNext(startCommandFilter).setNext(activityFilter)
                .setNext(distributionFilter)
                .setNext(techWorkFilter).setNext(updatesHandlerFilter);

        return updateFilter;
    }
}
