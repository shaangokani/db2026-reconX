package com.dbtraining.reconx.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TICKET-ADV084 — without this bean, @Timed is an inert marker annotation:
 * Spring only proxies it if a TimedAspect is registered to advise it. Not
 * auto-configured by spring-boot-starter-actuator.
 */
@Configuration
public class MetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
