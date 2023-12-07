package com.springboot.sample.app.consumer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


import io.micrometer.tracing.Tracer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;


@SpringBootTest
@EmbeddedKafka(partitions = 1)
@TestMethodOrder(MethodOrderer.MethodName.class)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}"
        , "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class KafkaConsumerTest {

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    KafkaListenerEndpointRegistry endpointRegistry;

    @Value("${spring.kafka.consumer.topic}")
    private String topicName;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
    private Tracer tracer;
    @BeforeEach
    void setUp() {
        for (MessageListenerContainer messageListenerContainer : endpointRegistry.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(messageListenerContainer,
                    embeddedKafkaBroker.getPartitionsPerTopic());
        }
    }
    @Test
    void onMessage() throws ExecutionException, InterruptedException {
        Message<String> kafkaMessage = MessageBuilder
                .withPayload("================================> Good Morning")
                .setHeader(KafkaHeaders.TOPIC, topicName)
               .setHeader("X-B3-TraceId", tracer.currentSpan().context().traceId()) // Set traceId
                .build();
        CompletableFuture<SendResult<String, String>> rs = kafkaTemplate.send(kafkaMessage);
        RecordMetadata recordMetadata = rs.get().getRecordMetadata();
        System.out.println(recordMetadata.partition() + " "+ recordMetadata.offset());

    }
}