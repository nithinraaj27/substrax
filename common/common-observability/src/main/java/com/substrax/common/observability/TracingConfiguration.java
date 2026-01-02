package com.substrax.common.observability;

import io.micrometer.tracing.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfiguration {

    @Bean
    public TracingContextHolder tracingContextHolder(Tracer tracer){
        return new TracingContextHolder(tracer);
    }
}
