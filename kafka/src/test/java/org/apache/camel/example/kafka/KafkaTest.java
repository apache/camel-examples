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
package org.apache.camel.example.kafka;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.infra.kafka.services.KafkaService;
import org.apache.camel.test.infra.kafka.services.KafkaServiceFactory;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.apache.camel.example.kafka.MessagePublisherClient.setUpKafkaComponent;
import static org.apache.camel.util.PropertiesHelper.asProperties;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel can produce and consume messages to / from a Kafka broker.
 */
class KafkaTest extends CamelTestSupport {

    @RegisterExtension
    private static final KafkaService SERVICE = KafkaServiceFactory.createService();

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        // Set the location of the configuration
        camelContext.getPropertiesComponent().setLocation("classpath:application.properties");

        // Override the host and port of the broker
        camelContext.getPropertiesComponent().setOverrideProperties(
            asProperties(
                "kafka.brokers", SERVICE.getBootstrapServers()
            )
        );
        setUpKafkaComponent(camelContext);
        return camelContext;
    }

    public void setupResources() throws Exception {
        // Replace the from endpoint to send messages easily
    	// still use the deprecated method for now as method is not visible camelContextConfiguration().replaceRouteFromWith("input", "direct:in");
        replaceRouteFromWith("input", "direct:in");
        super.setupResources();
    }

    @Test
    void should_exchange_messages_with_a_kafka_broker() throws Exception {
        String message = UUID.randomUUID().toString();
        template.sendBody("direct:in", message);
        NotifyBuilder notify = new NotifyBuilder(context).fromRoute("FromKafka")
                .whenCompleted(1).whenBodiesReceived(message).create();
        assertTrue(
            notify.matches(20, TimeUnit.SECONDS), "1 message should be completed"
        );
    }

    @Override
    protected RoutesBuilder[] createRouteBuilders() {
        return new RoutesBuilder[]{
            MessageConsumerClient.createRouteBuilder(), MessagePublisherClient.createRouteBuilder()
        };
    }
}
