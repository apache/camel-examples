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

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.caffeine.resume.single.CaffeineCache;
import org.apache.camel.component.file.consumer.GenericFileResumeAdapter;
import org.apache.camel.component.file.consumer.adapters.DefaultGenericFileResumeAdapter;
import org.apache.camel.example.resume.clients.kafka.DefaultConsumerPropertyFactory;
import org.apache.camel.example.resume.clients.kafka.DefaultProducerPropertyFactory;
import org.apache.camel.example.resume.clients.kafka.FileDeserializer;
import org.apache.camel.example.resume.clients.kafka.FileSerializer;
import org.apache.camel.example.resume.strategies.kafka.file.LargeFileRouteBuilder;
import org.apache.camel.main.Main;
import org.apache.camel.processor.resume.kafka.SingleNodeKafkaResumeStrategy;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;

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
        SingleNodeKafkaResumeStrategy<File, File> resumeStrategy = getUpdatableConsumerResumeStrategy();

        RouteBuilder routeBuilder = new LargeFileRouteBuilder(resumeStrategy, latch);
        main.configure().addRoutesBuilder(routeBuilder);

        Executors.newSingleThreadExecutor().submit(() -> waitForStop(main, latch));

        main.run(args);
    }

    private static SingleNodeKafkaResumeStrategy<File, File> getUpdatableConsumerResumeStrategy() {
        String bootStrapAddress = System.getProperty("bootstrap.address", "localhost:9092");
        String kafkaTopic = System.getProperty("resume.type.kafka.topic", "offsets");

        final DefaultConsumerPropertyFactory consumerPropertyFactory = new DefaultConsumerPropertyFactory(bootStrapAddress);

        consumerPropertyFactory.setKeyDeserializer(FileDeserializer.class.getName());
        consumerPropertyFactory.setValueDeserializer(LongDeserializer.class.getName());

        // In this case, we want to consume only the most recent offset
        consumerPropertyFactory.setOffsetReset("latest");

        final DefaultProducerPropertyFactory producerPropertyFactory = new DefaultProducerPropertyFactory(bootStrapAddress);

        producerPropertyFactory.setKeySerializer(FileSerializer.class.getName());
        producerPropertyFactory.setValueSerializer(LongSerializer.class.getName());

        CaffeineCache<File,Long> cache = new CaffeineCache<>(1);

        GenericFileResumeAdapter adapter = new DefaultGenericFileResumeAdapter(cache);

        return new SingleNodeKafkaResumeStrategy(kafkaTopic, cache, adapter, producerPropertyFactory.getProperties(),
                consumerPropertyFactory.getProperties());
    }

    private static void waitForStop(Main main, CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        main.stop();
    }



}

