/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.example.resume.fileset.clusterized.strategies;

import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.camel.example.resume.clients.kafka.DefaultConsumerPropertyFactory;
import org.apache.camel.example.resume.clients.kafka.DefaultProducerPropertyFactory;
import org.apache.camel.example.resume.strategies.kafka.fileset.KafkaFileSetResumeStrategy;
import org.apache.camel.example.resume.strategies.kafka.fileset.MultiItemCache;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterAwareKafkaFileSetResumeStrategy<K, V> extends KafkaFileSetResumeStrategy<K,V> {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterAwareKafkaFileSetResumeStrategy.class);

    private final DefaultConsumerPropertyFactory consumerPropertyFactory;
    private final ExecutorService executorService;

    public ClusterAwareKafkaFileSetResumeStrategy(String topic, MultiItemCache cache, DefaultProducerPropertyFactory producerPropertyFactory, DefaultConsumerPropertyFactory consumerPropertyFactory) {
        super(topic, cache, producerPropertyFactory, consumerPropertyFactory);
        this.consumerPropertyFactory = consumerPropertyFactory;

        // We need to keep refreshing the cache
        executorService = Executors.newSingleThreadExecutor();
        LOG.trace("Creating a offset cache refresher");
        executorService.submit(() -> refresh());
    }

    // We use this to continually refresh the local cache with last processed offsets from the other node
    public void refresh() {

        try {
            Properties prop = (Properties) consumerPropertyFactory.getProperties().clone();
            prop.setProperty(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());

            Consumer<K, V> consumer = new KafkaConsumer<>(prop);

            consumer.subscribe(Collections.singletonList(getTopic()));

            while (true) {
                var records = consumer.poll(getPollDuration());
                if (records.isEmpty()) {
                    continue;
                }

                for (var record : records) {
                    V value = record.value();

                    LOG.trace("Read from Kafka: {}", value);
                    getCache().add(record.key(), record.value());
                }
            }
        } catch (Exception e) {
            LOG.error("Error while refreshing the local cache: {}", e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        try {
            executorService.shutdown();
        } finally {
            super.stop();
        }
    }
}
