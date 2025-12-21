package com.sparta.ecommerce.config;

import com.sparta.ecommerce.application.coupon.event.CouponIssueEvent;
import com.sparta.ecommerce.application.coupon.event.CouponRestoreEvent;
import com.sparta.ecommerce.application.order.event.OrderCreatedEvent;
import com.sparta.ecommerce.application.product.event.StockRestoreEvent;
import com.sparta.ecommerce.application.user.event.PointRestoreEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 설정
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * 쿠폰 발급 토픽 생성
     * - 파티션 3개: Consumer concurrency와 매칭하여 병렬 처리 가능
     * - 복제본 1개: 테스트 환경에서는 단일 브로커 사용
     */
    @Bean
    public NewTopic couponIssueTopic() {
        return TopicBuilder.name("coupon-issue-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Kafka Consumer Factory 설정
     */
    @Bean
    public ConsumerFactory<String, CouponIssueEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CouponIssueEvent.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new JsonDeserializer<>(CouponIssueEvent.class, false)
        );
    }

    /**
     * Kafka Listener Container Factory 설정
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CouponIssueEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CouponIssueEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    /**
     * Kafka Producer Factory 설정
     */
    @Bean
    public ProducerFactory<String, CouponIssueEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Kafka Template 설정
     */
    @Bean
    public KafkaTemplate<String, CouponIssueEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ========== 주문 생성 이벤트 관련 설정 ==========

    /**
     * 주문 생성 토픽 생성
     * - 파티션 3개: Consumer concurrency와 매칭하여 병렬 처리 가능
     * - 복제본 1개: 테스트 환경에서는 단일 브로커 사용
     */
    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name("order-created-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * 주문 생성 이벤트용 Kafka Consumer Factory 설정
     */
    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> orderCreatedConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderCreatedEvent.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new JsonDeserializer<>(OrderCreatedEvent.class, false)
        );
    }

    /**
     * 주문 생성 이벤트용 Kafka Listener Container Factory 설정
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> orderCreatedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderCreatedConsumerFactory());
        return factory;
    }

    /**
     * 주문 생성 이벤트용 Kafka Producer Factory 설정
     */
    @Bean
    public ProducerFactory<String, OrderCreatedEvent> orderCreatedProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * 주문 생성 이벤트용 Kafka Template 설정
     */
    @Bean
    public KafkaTemplate<String, OrderCreatedEvent> orderCreatedKafkaTemplate() {
        return new KafkaTemplate<>(orderCreatedProducerFactory());
    }

    // ========== 보상 트랜잭션 이벤트 관련 설정 ==========

    /**
     * 재고 복구 토픽 생성
     * - 파티션 3개: 병렬 처리 가능
     * - 복제본 1개: 테스트 환경에서는 단일 브로커 사용
     */
    @Bean
    public NewTopic stockRestoreTopic() {
        return TopicBuilder.name("stock-restore-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * 재고 복구 이벤트용 Kafka Consumer Factory 설정
     */
    @Bean
    public ConsumerFactory<String, StockRestoreEvent> stockRestoreConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, StockRestoreEvent.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new JsonDeserializer<>(StockRestoreEvent.class, false)
        );
    }

    /**
     * 재고 복구 이벤트용 Kafka Listener Container Factory 설정
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StockRestoreEvent> stockRestoreKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, StockRestoreEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stockRestoreConsumerFactory());
        return factory;
    }

    /**
     * 재고 복구 이벤트용 Kafka Producer Factory 설정
     */
    @Bean
    public ProducerFactory<String, StockRestoreEvent> stockRestoreProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * 재고 복구 이벤트용 Kafka Template 설정
     */
    @Bean
    public KafkaTemplate<String, StockRestoreEvent> stockRestoreKafkaTemplate() {
        return new KafkaTemplate<>(stockRestoreProducerFactory());
    }

    // ========== 포인트 복구 이벤트 관련 설정 ==========

    /**
     * 포인트 복구 토픽 생성
     * - 파티션 3개: 병렬 처리 가능
     * - 복제본 1개: 테스트 환경에서는 단일 브로커 사용
     */
    @Bean
    public NewTopic pointRestoreTopic() {
        return TopicBuilder.name("point-restore-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * 포인트 복구 이벤트용 Kafka Consumer Factory 설정
     */
    @Bean
    public ConsumerFactory<String, PointRestoreEvent> pointRestoreConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PointRestoreEvent.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new JsonDeserializer<>(PointRestoreEvent.class, false)
        );
    }

    /**
     * 포인트 복구 이벤트용 Kafka Listener Container Factory 설정
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PointRestoreEvent> pointRestoreKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PointRestoreEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(pointRestoreConsumerFactory());
        return factory;
    }

    /**
     * 포인트 복구 이벤트용 Kafka Producer Factory 설정
     */
    @Bean
    public ProducerFactory<String, PointRestoreEvent> pointRestoreProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * 포인트 복구 이벤트용 Kafka Template 설정
     */
    @Bean
    public KafkaTemplate<String, PointRestoreEvent> pointRestoreKafkaTemplate() {
        return new KafkaTemplate<>(pointRestoreProducerFactory());
    }

    // ========== 쿠폰 복구 이벤트 관련 설정 ==========

    /**
     * 쿠폰 복구 토픽 생성
     * - 파티션 3개: 병렬 처리 가능
     * - 복제본 1개: 테스트 환경에서는 단일 브로커 사용
     */
    @Bean
    public NewTopic couponRestoreTopic() {
        return TopicBuilder.name("coupon-restore-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * 쿠폰 복구 이벤트용 Kafka Consumer Factory 설정
     */
    @Bean
    public ConsumerFactory<String, CouponRestoreEvent> couponRestoreConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CouponRestoreEvent.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new JsonDeserializer<>(CouponRestoreEvent.class, false)
        );
    }

    /**
     * 쿠폰 복구 이벤트용 Kafka Listener Container Factory 설정
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CouponRestoreEvent> couponRestoreKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CouponRestoreEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(couponRestoreConsumerFactory());
        return factory;
    }

    /**
     * 쿠폰 복구 이벤트용 Kafka Producer Factory 설정
     */
    @Bean
    public ProducerFactory<String, CouponRestoreEvent> couponRestoreProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * 쿠폰 복구 이벤트용 Kafka Template 설정
     */
    @Bean
    public KafkaTemplate<String, CouponRestoreEvent> couponRestoreKafkaTemplate() {
        return new KafkaTemplate<>(couponRestoreProducerFactory());
    }
}
