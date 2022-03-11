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

package org.apache.camel.example.resume.fileset.strategies;

import java.io.File;

import org.apache.camel.component.file.consumer.FileResumeSet;
import org.apache.camel.component.file.consumer.FileSetResumeStrategy;
import org.apache.camel.example.resume.clients.kafka.DefaultConsumerPropertyFactory;
import org.apache.camel.example.resume.clients.kafka.DefaultProducerPropertyFactory;
import org.apache.camel.processor.resume.kafka.AbstractKafkaResumeStrategy;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaFileSetResumeStrategy<K, V> extends AbstractKafkaResumeStrategy<K, V> implements FileSetResumeStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaFileSetResumeStrategy.class);

    private final MultiItemCache<K, V> cache;
    private final String topic;

    public KafkaFileSetResumeStrategy(String topic, MultiItemCache<K, V> cache,
                                      DefaultProducerPropertyFactory producerPropertyFactory,
                                      DefaultConsumerPropertyFactory consumerPropertyFactory)
    {
        super(topic, cache, producerPropertyFactory.getProperties(), consumerPropertyFactory.getProperties());
        this.topic = topic;
        this.cache = cache;
    }

    @Override
    public ConsumerRecords<K, V> consume() {
        return consume(10);
    }

    private boolean notProcessed(File file) {
        File key = file.getParentFile();

        // if the file is in the cache, then it's already processed
        boolean ret = !cache.contains((K) key, (V) file);
        return ret;
    }

    @Override
    public void subscribe() {
        checkAndSubscribe(topic);
    }

    @Override
    public void resume(FileResumeSet resumable) {
        if (resumable != null) {
            resumable.resumeEach(this::notProcessed);
            if (resumable.hasResumables()) {
                LOG.debug("There's {} files to still to be processed", resumable.resumed().length);
            }
        } else {
            LOG.trace("Nothing to resume");
        }
    }

    @Override
    public void resume() {
        throw new UnsupportedOperationException("Cannot perform blind resume");
    }
}
