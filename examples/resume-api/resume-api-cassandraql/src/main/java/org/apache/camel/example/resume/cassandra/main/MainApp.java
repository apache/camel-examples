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

package org.apache.camel.example.resume.cassandra.main;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.apache.camel.component.caffeine.resume.CaffeineCache;
import org.apache.camel.main.Main;
import org.apache.camel.processor.resume.kafka.SingleNodeKafkaResumeStrategy;
import org.apache.camel.resume.Resumable;
import org.apache.kafka.clients.consumer.ConsumerConfig;

public class MainApp {
    public static void main(String[] args) {
        String host = System.getProperty("cassandra.host", "localhost");
        String txtPort = System.getProperty("cassandra.cql3.port", "9042");

        int port = Integer.valueOf(txtPort);

        // Runs the load action
        String action = System.getProperty("resume.action");
        if ("load".equalsIgnoreCase(action)) {
            try {
                loadData(host, port);
                System.exit(0);
            } catch (Exception e) {
                System.err.println("Unable to load data: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        // Normal code path for consuming from Cassandra
        SingleNodeKafkaResumeStrategy<Resumable> resumeStrategy = getUpdatableConsumerResumeStrategyForSet();

        Main main = new Main();

        Integer batchSize = Integer.parseInt(System.getProperty("batch.size", "50"));
        CountDownLatch latch = new CountDownLatch(batchSize);
        Executors.newSingleThreadExecutor().submit(() -> waitForStop(main, latch));


        try (CassandraClient client = new CassandraClient(host, port)) {
            main.configure().addRoutesBuilder(new CassandraRoute(latch, batchSize, resumeStrategy, new CaffeineCache<>(10240), client));
            main.start();
        }
    }

    public static void loadData(String host, int port) {
        try (CassandraClient cassandraClient = new CassandraClient(host, port)) {
            ExampleDao exampleDao = cassandraClient.newExampleDao();

            exampleDao.createKeySpace();
            exampleDao.useKeySpace();
            exampleDao.createTable();

            int dataSize = Integer.parseInt(System.getProperty("data.size", "500"));

            for (long i = 0; i < dataSize; i++) {
                exampleDao.insert(i, UUID.randomUUID().toString());
            }
        }
    }

    private static SingleNodeKafkaResumeStrategy<Resumable> getUpdatableConsumerResumeStrategyForSet() {
        String bootStrapAddress = System.getProperty("bootstrap.address", "localhost:9092");
        String kafkaTopic = System.getProperty("resume.type.kafka.topic", "offsets");

        final Properties consumerProperties = SingleNodeKafkaResumeStrategy.createConsumer(bootStrapAddress);
        consumerProperties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        final Properties producerProperties = SingleNodeKafkaResumeStrategy.createProducer(bootStrapAddress);
        return new SingleNodeKafkaResumeStrategy<>(kafkaTopic, producerProperties, consumerProperties);
    }

    private static void waitForStop(Main main, CountDownLatch latch) {
        try {
            latch.await();

            main.stop();
            while (!main.isStopped()) {
                Thread.sleep(1000);
            }

            System.exit(0);
        } catch (InterruptedException e) {
            System.exit(1);
        }
    }
}
