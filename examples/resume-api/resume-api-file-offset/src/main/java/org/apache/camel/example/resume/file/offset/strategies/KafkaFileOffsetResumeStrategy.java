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

package org.apache.camel.example.resume.file.offset.strategies;

import java.io.File;
import java.util.Optional;

import org.apache.camel.ResumeCache;
import org.apache.camel.component.file.consumer.GenericFileResumable;
import org.apache.camel.component.file.consumer.GenericFileResumeStrategy;
import org.apache.camel.example.resume.clients.kafka.DefaultConsumerPropertyFactory;
import org.apache.camel.example.resume.clients.kafka.DefaultProducerPropertyFactory;
import org.apache.camel.processor.resume.kafka.AbstractKafkaResumeStrategy;

public class KafkaFileOffsetResumeStrategy<K> extends AbstractKafkaResumeStrategy<K, Long> implements GenericFileResumeStrategy<File> {
    public static final int CACHE_SIZE = 100;

    private final String topic;
    private final ResumeCache<K, Long> cache;

    // NOTE: To have data type flexibility, we need to allow callers to set up the consumer and producer property
    // factories. See MainApp.
    public KafkaFileOffsetResumeStrategy(String topic,
                                         ResumeCache<K, Long> cache,
                                         DefaultProducerPropertyFactory producerPropertyFactory,
                                         DefaultConsumerPropertyFactory consumerPropertyFactory)
    {
        super(topic, cache, producerPropertyFactory.getProperties(), consumerPropertyFactory.getProperties());
        this.topic = topic;
        this.cache = cache;
    }


    private Optional<Long> getLastOffset(GenericFileResumable<File> resumable) {
        final File addressable = resumable.getAddressable();
        return getLastOffset((K) addressable);
    }

    public Optional<Long> getLastOffset(K addressable) {
        return cache.get(addressable);
    }

    @Override
    public void subscribe() {
        checkAndSubscribe(topic, 1);
    }

    @Override
    public void resume(GenericFileResumable<File> resumable) {
        final Optional<Long> lastOffsetOpt = getLastOffset(resumable);

        if (!lastOffsetOpt.isPresent()) {
            return;
        }

        final long lastOffset = lastOffsetOpt.get();
        resumable.updateLastOffset(lastOffset);
    }

    @Override
    public void resume() {
        throw new UnsupportedOperationException("Cannot perform blind resume");
    }
}
