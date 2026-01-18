package com.substrax.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory){
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        template.setObservationEnabled(true);
        return template;
    }
}
