package com.javanext.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topic.orders}")
    private String ordersTopic;

    @Value("${kafka.topic.payments}")
    private String paymentsTopic;

    @Value("${kafka.topic.inventory}")
    private String inventoryTopic;

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(ordersTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentsTopic() {
        return TopicBuilder.name(paymentsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryTopic() {
        return TopicBuilder.name(inventoryTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
