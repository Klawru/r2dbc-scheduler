package io.gitlab.klawru.scheduler.boot;

import io.gitlab.klawru.scheduler.SchedulerClient;
import io.gitlab.klawru.scheduler.boot.actuator.SchedulerClientHealthIndicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnBean(SchedulerClient.class)
@ConditionalOnEnabledHealthIndicator("r2dbc-scheduler")
@ConditionalOnClass(HealthContributorAutoConfiguration.class)
@AutoConfigureAfter({
        HealthEndpointAutoConfiguration.class,
        R2dbcSchedulerAutoConfiguration.class,
})
public class R2dbcSchedulerActuatorAutoConfiguration {

    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    public HealthIndicator schedulerClientHealthIndicator(SchedulerClient scheduler) {
        log.debug("Exposing health indicator for db-scheduler");
        return new SchedulerClientHealthIndicator(scheduler);
    }
}
