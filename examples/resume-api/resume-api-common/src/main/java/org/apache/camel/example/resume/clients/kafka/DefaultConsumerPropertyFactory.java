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

package org.apache.camel.example.resume.clients.kafka;

import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;


/**
 * A property producer that can be used to create a Kafka consumer with a minimum
 * set of configurations that can consume from a Kafka topic.
 * <p>
 * The consumer behavior from using this set of properties causes the consumer to
 * consumes all published messages "from-beginning".
 */
public class DefaultConsumerPropertyFactory implements ConsumerPropertyFactory {
    private final String bootstrapServer;
    private String valueDeserializer = StringDeserializer.class.getName();
    private String keyDeserializer = StringDeserializer.class.getName();
    private String offsetReset = "earliest";

    /**
     * Constructs the properties using the given bootstrap server
     *
     * @param bootstrapServer the address of the server in the format
     *                        PLAINTEXT://${address}:${port}
     */
    public DefaultConsumerPropertyFactory(String bootstrapServer) {
        this.bootstrapServer = bootstrapServer;
    }

    public void setValueDeserializer(String valueDeserializer) {
        this.valueDeserializer = valueDeserializer;
    }

    public void setKeyDeserializer(String keyDeserializer) {
        this.keyDeserializer = keyDeserializer;
    }

    public void setOffsetReset(String offsetReset) {
        this.offsetReset = offsetReset;
    }

    @Override
    public Properties getProperties() {
        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());

        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);
        return props;
    }
}
