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

package org.apache.camel.example.resume.file.offset.main;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.caffeine.resume.CaffeineCache;
import org.apache.camel.example.resume.strategies.kafka.file.LargeFileRouteBuilder;
import org.apache.camel.main.Main;
import org.apache.camel.processor.resume.kafka.SingleNodeKafkaResumeStrategy;
import org.apache.camel.resume.Resumable;
import org.apache.kafka.clients.consumer.ConsumerConfig;

/**
 * A Camel Application
 */
public class MainApp {

    /**
     * A main() so we can easily run these routing rules in our IDE
     */
    public static void main(String... args) throws Exception {
        Main main = new Main();

        CountDownLatch latch = new CountDownLatch(1);
        SingleNodeKafkaResumeStrategy<Resumable> resumeStrategy = getUpdatableConsumerResumeStrategy();

        RouteBuilder routeBuilder = new LargeFileRouteBuilder(resumeStrategy, new CaffeineCache<>(100), latch);
        main.configure().addRoutesBuilder(routeBuilder);

        Executors.newSingleThreadExecutor().submit(() -> waitForStop(main, latch));

        main.run(args);
    }

    private static SingleNodeKafkaResumeStrategy<Resumable> getUpdatableConsumerResumeStrategy() {
        String bootStrapAddress = System.getProperty("bootstrap.address", "localhost:9092");
        String kafkaTopic = System.getProperty("resume.type.kafka.topic", "offsets");

        final Properties consumerProperties = SingleNodeKafkaResumeStrategy.createConsumer(bootStrapAddress);

        // In this case, we want to consume only the most recent offset
        consumerProperties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        final Properties producerProperties = SingleNodeKafkaResumeStrategy.createProducer(bootStrapAddress);

        return new SingleNodeKafkaResumeStrategy(kafkaTopic, producerProperties, consumerProperties);
    }

    private static void waitForStop(Main main, CountDownLatch latch) {
        try {
            latch.await();
            System.exit(0);
        } catch (InterruptedException e) {
            System.exit(1);
        }
    }



}

