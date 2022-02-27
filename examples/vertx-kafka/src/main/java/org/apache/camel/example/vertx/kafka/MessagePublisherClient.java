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
package org.apache.camel.example.vertx.kafka;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.component.ComponentsBuilderFactory;
import org.apache.camel.component.vertx.kafka.VertxKafkaConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MessagePublisherClient {

    private static final Logger LOG = LoggerFactory.getLogger(MessagePublisherClient.class);
    public static final String DIRECT_KAFKA_START = "direct:kafkaStart";
    public static final String DIRECT_KAFKA_START_WITH_PARTITIONER = "direct:kafkaStartWithPartitioner";
    public static final String HEADERS = "${headers}";

    private MessagePublisherClient() { }

    public static void main(String[] args) throws Exception {

        LOG.info("About to run Camel Vertx Kafka integration...");

        String testKafkaMessage = "Test Message from  MessagePublisherClient " + Calendar.getInstance().getTime();
        try (CamelContext camelContext = new DefaultCamelContext()) {
            // Set the location of the configuration
            camelContext.getPropertiesComponent().setLocation("classpath:application.properties");
            // Set up the Kafka component
            setUpKafkaComponent(camelContext);
            // Add route to send messages to Kafka
            camelContext.addRoutes(createRouteBuilder());

            try (ProducerTemplate producerTemplate = camelContext.createProducerTemplate()) {
                camelContext.start();

                Map<String, Object> headers = new HashMap<>();

                headers.put(VertxKafkaConstants.PARTITION_ID, 0);
                headers.put(VertxKafkaConstants.MESSAGE_KEY, "1");
                producerTemplate.sendBodyAndHeaders(DIRECT_KAFKA_START, testKafkaMessage, headers);

                // Send with topicName in header

                testKafkaMessage = "TOPIC " + testKafkaMessage;
                headers.put(VertxKafkaConstants.MESSAGE_KEY, "2");
                headers.put(VertxKafkaConstants.TOPIC, "TestLog");

                producerTemplate.sendBodyAndHeaders("direct:kafkaStartNoTopic", testKafkaMessage, headers);

                testKafkaMessage = "PART 0 :  " + testKafkaMessage;
                Map<String, Object> newHeader = new HashMap<>();
                newHeader.put(VertxKafkaConstants.MESSAGE_KEY, "AB"); // This should go to partition 0

                producerTemplate.sendBodyAndHeaders(DIRECT_KAFKA_START_WITH_PARTITIONER, testKafkaMessage, newHeader);
            }

            LOG.info("Successfully published event to Kafka.");
            System.out.println("Enter text on the line below : [Press Ctrl-C to exit.] ");

            Thread.sleep(5L * 60 * 1_000);
        }
    }

    static RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                from(DIRECT_KAFKA_START).routeId("DirectToKafka")
                        .to("vertx-kafka:{{producer.topic}}").log(HEADERS);

                // Topic can be set in header as well.

                from("direct:kafkaStartNoTopic").routeId("kafkaStartNoTopic")
                        .to("vertx-kafka:dummy")
                        .log(HEADERS);

                // Use custom partitioner based on the key.

                from(DIRECT_KAFKA_START_WITH_PARTITIONER).routeId("kafkaStartWithPartitioner")
                        .to("vertx-kafka:{{producer.topic}}?partitionerClass={{producer.partitioner}}")
                        .log(HEADERS);


                // Takes input from the command line.

                from("stream:in").id("input").setHeader(VertxKafkaConstants.PARTITION_ID, simple("0"))
                        .setHeader(VertxKafkaConstants.MESSAGE_KEY, simple("1")).to(DIRECT_KAFKA_START);
            }
        };
    }

    static void setUpKafkaComponent(CamelContext camelContext) {
        // setup kafka component with the brokers using component DSL
        ComponentsBuilderFactory.vertxKafka()
                .bootstrapServers("{{kafka.brokers}}")
                .register(camelContext, "vertx-kafka");
    }
}
