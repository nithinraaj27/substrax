package com.substrax.common.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfiguration {

    private final MeterRegistry meterRegistry;

    @Value("${spring.aplication.name:unknown-service")
    private String serviceName;


    public MetricsConfiguration(MeterRegistry registry){
        this.meterRegistry = registry;
    }


    public void configurationCommonTags(){
        meterRegistry.config().commonTags(
                "service", serviceName
        );
    }
}
