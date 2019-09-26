package com.kafkaTest.services;

import com.kafkaTest.entity.Counter;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.UUID;

@Component
public class LeaderElection {

    private final CounterService counterService;
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderElection.class);

    @Autowired
    public LeaderElection(CounterService counterService) {
        this.counterService = counterService;
    }

    @Value("${cluster.zkhost}")
    private String ZKHost;
    @Value("${kafka.topic}")
    private String TOPIC;
    @Value("${kafka.batchSize}")
    private int batchSize;

    @EventListener({ApplicationStartedEvent.class})
    public void handleContextStartedEvent() {
        LOGGER.info("Starting leader election");
        startLeaderElection();
    }

    private void startLeaderElection() {
        int sleepMsBetweenRetries = 100;
        int maxRetries = 3;
        RetryPolicy retryPolicy = new RetryNTimes(maxRetries, sleepMsBetweenRetries);

        CuratorFramework client = CuratorFrameworkFactory.newClient(ZKHost, 5000, 5000, retryPolicy);
        client.start();
        LeaderSelector leaderSelector = new LeaderSelector(client,
                "/mutex/leader",
                new LeaderSelectorListener() {
                    @Override
                    public void stateChanged(CuratorFramework client, ConnectionState newState) {
                        LOGGER.info("State changed to " + newState.toString());
                    }

                    @Override
                    public void takeLeadership(CuratorFramework client) throws Exception {
                        LOGGER.info("Took leadership");
                        pushOuterMessagesToKafka();
                    }
                });

        leaderSelector.autoRequeue();
        leaderSelector.start();
    }

    private void pushOuterMessagesToKafka() throws InterruptedException {
        KafkaProducer<String, String> producer = getProducer();
        producer.initTransactions();
        while (true) {
            Thread.sleep(2000);
            try {
                producer.beginTransaction();
                Counter counter = counterService.getCounter(TOPIC);
                int count = counter.getCounter();
                //imitating getting messages from outer service.
                for (int i = count; i < count + batchSize; i++) {
                    producer.send(new ProducerRecord<>(TOPIC, String.valueOf(i), "message" + i));
                }
                counter.setCounter(count + batchSize);
                counterService.saveCounter(counter);
                producer.commitTransaction();
            } catch (Exception e) {
                producer.abortTransaction();
                LOGGER.error("Exception:", e);
            }
            LOGGER.info("Pushed " + batchSize + " messages to kafka");
        }
    }

    private KafkaProducer<String, String> getProducer() {
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", "localhost:9092");
        producerProps.put("enable.idempotence", "true");
        producerProps.put("transactional.id", UUID.randomUUID().toString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        KafkaProducer<String, String> producer = new KafkaProducer(producerProps);
        return producer;
    }
}
