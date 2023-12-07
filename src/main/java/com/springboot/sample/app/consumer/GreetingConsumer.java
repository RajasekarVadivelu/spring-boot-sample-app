package com.springboot.sample.app.consumer;


import static org.springframework.http.HttpMethod.GET;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.web.client.RestTemplate;


public class GreetingConsumer<K, V> {

    private static final Logger LOG = LoggerFactory.getLogger(GreetingConsumer.class);

    private final Tracer tracer;
    private final ObjectMapper objectMapper;

    private  final RestTemplate restTemplate;

    private final ObservationRegistry observationRegistry;

    public GreetingConsumer(Tracer tracer, ObservationRegistry observationRegistry, ObjectMapper mapper,
                            RestTemplate restTemplate) {
        this.tracer = tracer;
        this.observationRegistry = observationRegistry;
        this.objectMapper = mapper;
        this.restTemplate = restTemplate;
    }

    @KafkaListener(topics = "${spring.kafka.consumer.topic}", groupId = "${spring.kafka.consumer.group-id}",
            idIsGroup = false, containerFactory = "concurrentKafkaListener")
    public void onMessage(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {

        Observation.createNotStarted("on-message", this.observationRegistry).observe(() -> {
            if (consumerRecord.headers().headers("X-B3-TraceId").iterator().hasNext()) {
                String traceId = String.valueOf(consumerRecord.headers()
                        .headers("X-B3-TraceId").iterator().next().value());
                LOG.info("Message Received wit X-B3-TraceId:{} Key{} value{}",
                        traceId, consumerRecord.key(), consumerRecord.value());

            }
            TestEvent testEvent = null;
            try {
                testEvent = objectMapper.readValue(consumerRecord.value(), TestEvent.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            LOG.info("Received message", testEvent.getType());
            getAPIResponse();
            LOG.info("<ACCEPTANCE_TEST> <TRACE:{}> Hello from consumer",
                    this.tracer.currentSpan().context().traceId());
            acknowledgment.acknowledge();

        });

    }

    private void getAPIResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080",
                GET, entity,
                String.class);
        if(response.getStatusCode().isSameCodeAs(HttpStatus.OK)) {
            LOG.info("Response from REST call:{}", response.getBody());
        }
    }
}
