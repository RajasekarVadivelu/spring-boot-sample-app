package com.springboot.sample.app.consumer.config;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;

import java.io.IOException;
import java.io.ObjectInput;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.sample.app.ApplicationConstants;
import com.springboot.sample.app.consumer.GreetingConsumer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.web.client.RestTemplate;

@EnableKafka
@Configuration
public class KafkaConfig {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConfig.class);

    private static final String SASL_TRUSTSTORE_LOCATION = "ssl.truststore.location";
    private static final String SASL_TRUSTSTORE_PWORD = "ssl.truststore.password";

    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String consumerBootStrapServer;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.topic}")
    private String topicName;

    @Value("${spring.kafka.consumer.retryMaxIntervalSecs}")
    private Integer retryMaxIntervalSecs;

    @Value("${spring.kafka.consumer.maxElapsedTime}")
    private Integer maxElapsedTime;

    @Value("${spring.kafka.consumer.retryInitialIntervalSecs}")
    private Integer retryInitialIntervalSecs;

    @Value("${spring.kafka.consumer.pollTimeout}")
    private Integer pollTimeout;

    @Value("${spring.kafka.consumer.username}")
    private String consumerUsername;
    @Value("${spring.kafka.consumer.connectionString}")
    private String consumerConnectionString;
    @Value("${spring.kafka.consumer.login-module}")
    private String loginModule;
    @Value("${spring.kafka.consumer.sasl-mechanism}")
    private String saslMechanism;
    @Value("${spring.kafka.consumer.security-protocol}")
    private String securityProtocol;
    @Value("${spring.kafka.consumer.truststore-location}")
    private String truststoreLocation;
    @Value("${spring.kafka.consumer.truststore-password}")
    private String truststorePassword;
    @Value("${spring.kafka.consumer.offset-auto-reset}")
    private String consumerOffsetAutoReset;
    @Value("${spring.kafka.consumer.concurrency}")
    private int consumerConcurrency;
    @Value("${spring.kafka.consumer.client-config-id}")
    private String kafkaClientId;
    @Value("${spring.kafka.consumer.max-age}")
    private int consumerMaxAgeTime;
    @Value("${spring.kafka.consumer.request-timeout}")
    private int consumerRequestTimeout;
    @Value("${spring.kafka.producer.bootstrap-servers}")
    private String producerBootStrapServer;
    @Value("${spring.kafka.producer.deadlettertopic}")
    private String deadLetterTopic;
    @Value("${spring.kafka.producer.username}")
    private String producerUsername;
    @Value("${spring.kafka.producer.connectionString}")
    private  String producerConnectionString;
    @Value("${spring.kafka.producer.acks-config}")
    private String producerAcksConfig;
    @Value("${spring.kafka.producer.linger}")
    private int producerLinger;
    @Value("${spring.kafka.producer.request-timeout}")
    private int producerRequestTimeout;
    @Value("${spring.kafka.producer.batch-size}")
    private int producerBatchSize;
    @Value("${spring.kafka.producer.send-buffer}")
    private int producerSendBuffer;
    @Value("${spring.kafka.producer.retries}")
    private int producerRetries;
    @Value("${spring.kafka.producer.metadata-max-idle}")
    private int producerMetadataMaxIdleTime;
    @Value("${spring.kafka.producer.metadata-max-age}")
    private int producerMetadataMaxAgeTime;
    @Value("${spring.kafka.producer.connections-max-idle}")
    private int producerConnectionMaxIdle;
    @Value("${spring.kafka.producer.delivery-timeout}")
    private int producerDeliveryTimeout;

    @Autowired
    private MeterRegistry meterRegistry;

    @Bean
    GreetingConsumer greetingConsumer(Tracer tracer, ObservationRegistry observationRegistry, ObjectMapper objMapper,
                                      RestTemplate restTemplate) {
        return new GreetingConsumer<>(tracer, observationRegistry, objMapper, restTemplate);
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerBootStrapServer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, consumerRequestTimeout);
        props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, consumerMaxAgeTime);

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerOffsetAutoReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        addSaslProperties(props, saslMechanism, securityProtocol, loginModule, consumerUsername, consumerConnectionString);
        addTruststoreProperties(props, truststoreLocation, truststorePassword);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean(name = "concurrentKafkaListener")
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setPollTimeout(pollTimeout);
        factory.setCommonErrorHandler(errorHandlerWithDeadLetterRecoverer(retryWithDeadLetterRecoverer(kafkaTemplate())));
        factory.setConcurrency(consumerConcurrency);
        factory.getContainerProperties().setObservationEnabled(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
      //  factory.getConsumerFactory().addListener(new MicrometerConsumerListener<>(meterRegistry));
        return factory;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                producerBootStrapServer);
        configProps.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        configProps.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);


        configProps.put(ProducerConfig.LINGER_MS_CONFIG, producerLinger);
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, producerRequestTimeout);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, producerBatchSize);
        configProps.put(ProducerConfig.SEND_BUFFER_CONFIG, producerSendBuffer);
        configProps.put(ProducerConfig.ACKS_CONFIG, producerAcksConfig);
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, kafkaClientId);
        configProps.put(ProducerConfig.RETRIES_CONFIG, producerRetries);
        configProps.put(ProducerConfig.METADATA_MAX_AGE_CONFIG, producerMetadataMaxAgeTime);
        configProps.put(ProducerConfig.METADATA_MAX_IDLE_CONFIG, producerMetadataMaxIdleTime);
        configProps.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG,producerConnectionMaxIdle);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, producerDeliveryTimeout);
        addSaslProperties(configProps, saslMechanism, securityProtocol, loginModule, producerUsername, producerConnectionString);
        addTruststoreProperties(configProps, truststoreLocation, truststorePassword);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * This method is used to handle kafka error & processed to dead letter topic.
     *
     * @param recoverer of type DeadLetterPublishingRecoverer.
     * @return of type ErrorHandler.
     */
    private DefaultErrorHandler  errorHandlerWithDeadLetterRecoverer(DeadLetterPublishingRecoverer recoverer) {
        ExponentialBackOff exponentialBackOff = new ExponentialBackOff(retryInitialIntervalSecs, 2D);
        exponentialBackOff.setMaxInterval(retryMaxIntervalSecs);
        exponentialBackOff.setMaxElapsedTime(maxElapsedTime);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler ((consumerRecord, exception) -> {
            LOG.error("message with offset {} with key {} on DLT topic failed after all retries", consumerRecord.offset(), consumerRecord.key());
            recoverer.accept(consumerRecord, exception);
        }, exponentialBackOff);
        errorHandler.addRetryableExceptions( IOException.class,
                TimeoutException.class, KafkaException.class);
        errorHandler.addNotRetryableExceptions(UnmarshalException.class);
        errorHandler.setCommitRecovered(true);
        return errorHandler;
    }

    /**
     * This method is used to publish dead letter errors to dead letter topic.
     *
     * @param template of type KafkaTemplate.
     * @return of type DeadLetterPublishingRecoverer.
     */
    private DeadLetterPublishingRecoverer retryWithDeadLetterRecoverer(KafkaTemplate<String, String> template) {
        BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> biFunction = (consumerRecord, exception) -> {
            if (exception.getCause() instanceof JAXBException) {
                LOG.error("Unmarshaling xml message failed: {}. Hence sending message with offset{} to dead letter topic {}.",
                        exception.getCause(), consumerRecord.offset(), deadLetterTopic);
                setDefaultKeyIdentifier();
                setRetryHeaders(consumerRecord, ApplicationConstants.DLT_EXCEPTION_TYPE_NONRETRYABLE,
                        ApplicationConstants.DLT_EXCEPTION_LEVEL_TECHNICAL, exception.getMessage());
                return new TopicPartition(deadLetterTopic, consumerRecord.partition());
            } else if (exception.getCause() instanceof IllegalArgumentException) {
                LOG.error("Parsing of message with offset {} failed: {}. Hence sending message to dead letter topic {}.",
                        consumerRecord.offset(), exception.getCause(), deadLetterTopic);
                setRetryHeaders(consumerRecord, ApplicationConstants.DLT_EXCEPTION_TYPE_NONRETRYABLE,
                        ApplicationConstants.DLT_EXCEPTION_LEVEL_BUSINESS, exception.getMessage());
                return new TopicPartition(deadLetterTopic, consumerRecord.partition());
            } else {
                LOG.error("Error occurred {} while message processing. Hence sending message with offset{} to dead letter topic {}.",
                        exception.getCause(), consumerRecord.offset(), deadLetterTopic);
                setRetryHeaders(consumerRecord, ApplicationConstants.DLT_EXCEPTION_TYPE_RETRYABLE,
                        ApplicationConstants.DLT_EXCEPTION_LEVEL_TECHNICAL, exception.getMessage());
            }
            return new TopicPartition(deadLetterTopic, consumerRecord.partition());

        };
        return new DeadLetterPublishingRecoverer(template, biFunction);
    }

    private void setDefaultKeyIdentifier() {
        MDC.put(ApplicationConstants.KEY1_ID, "" );
        MDC.put(ApplicationConstants.KEY1_TYPE, "AgreementNr");
        MDC.put(ApplicationConstants.KEY2_ID, "");
        MDC.put(ApplicationConstants.KEY2_TYPE, "FarCustomerAddressIdentifier");
    }

    private void setRetryHeaders(ConsumerRecord<?, ?> consumerRecord, String exceptionTypeValue, String exceptionLevel, String message) {
        if (consumerRecord.headers().headers(ApplicationConstants.DLT_EXCEPTION_TYPE).iterator().hasNext()) {
            consumerRecord.headers().remove(ApplicationConstants.DLT_EXCEPTION_TYPE);
        }
        consumerRecord.headers().add(ApplicationConstants.DLT_EXCEPTION_TYPE, exceptionTypeValue.getBytes());
        consumerRecord.headers().add(ApplicationConstants.DLT_EXCEPTION_LEVEL, exceptionLevel.getBytes());
        consumerRecord.headers().add(ApplicationConstants.DLT_EXCEPTION_MESSAGE, message.getBytes());

        if (MDC.get(ApplicationConstants.KEY1_ID) != null) {
            consumerRecord.headers().add(ApplicationConstants.KEY1_ID, MDC.get(ApplicationConstants.KEY1_ID).getBytes());
        }
        if (MDC.get(ApplicationConstants.KEY1_TYPE) != null) {
            consumerRecord.headers().add(ApplicationConstants.KEY1_TYPE, MDC.get(ApplicationConstants.KEY1_TYPE).getBytes());
        }
        if (MDC.get(ApplicationConstants.KEY2_ID) != null) {
            consumerRecord.headers().add(ApplicationConstants.KEY2_ID, MDC.get(ApplicationConstants.KEY2_ID).getBytes());
        }
        if (MDC.get(ApplicationConstants.KEY2_TYPE) != null) {
            consumerRecord.headers().add(ApplicationConstants.KEY2_TYPE, MDC.get(ApplicationConstants.KEY2_TYPE).getBytes());
        }


        if (consumerRecord.headers().headers(ApplicationConstants.DLT_RETRY_COUNT).iterator().hasNext()){
            int retryCount = ByteBuffer.wrap(consumerRecord.headers().headers(ApplicationConstants.DLT_RETRY_COUNT)
                    .iterator().next().value()).getInt();
            retryCount = retryCount + 1;
            consumerRecord.headers().remove(ApplicationConstants.DLT_RETRY_COUNT);
            consumerRecord.headers().add(ApplicationConstants.DLT_RETRY_COUNT,
                    ByteBuffer.allocate(ApplicationConstants.DLT_RETRY_COUNT_BYTE_CAPACITY).putInt(retryCount).array());
        } else {
            consumerRecord.headers().add(ApplicationConstants.DLT_RETRY_COUNT,
                    ByteBuffer.allocate(ApplicationConstants.DLT_RETRY_COUNT_BYTE_CAPACITY).putInt(1).array());
        }
    }

    /**
     * This method is used to add SASL properties.
     *
     * @param properties       of type Map.
     * @param saslMechanism    of type String.
     * @param securityProtocol of type String.
     * @param loginModule      of type String.
     * @param username         of type String.
     * @param password         of type String.
     */
    public static void addSaslProperties(Map<String, Object> properties, String saslMechanism, String securityProtocol,
                                         String loginModule, String username, String password) {
        if (!StringUtils.isEmpty(username)) {
            properties.put(SECURITY_PROTOCOL_CONFIG, securityProtocol);
            properties.put(SASL_MECHANISM, saslMechanism);
            properties.put(SASL_JAAS_CONFIG, String.format("%s required username=\"%s\" password=\"%s\" ;", loginModule, username, password));
        }
    }

    /**
     * This method is used to store trust store properties.
     *
     * @param properties of type Map.
     * @param location   of type String.
     * @param password   of type String.
     */
    public static void addTruststoreProperties(Map<String, Object> properties, String location, String password) {
        if (!StringUtils.isEmpty(location)) {
            properties.put(SASL_TRUSTSTORE_LOCATION, location);
            properties.put(SASL_TRUSTSTORE_PWORD, password);
        }
    }
}
