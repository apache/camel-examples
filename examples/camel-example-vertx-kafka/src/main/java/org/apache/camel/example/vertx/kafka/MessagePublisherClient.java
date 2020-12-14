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

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.component.ComponentsBuilderFactory;
import org.apache.camel.component.vertx.kafka.VertxKafkaConstants;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MessagePublisherClient {
    
    private static final Logger LOG = LoggerFactory.getLogger(MessagePublisherClient.class);

    // use Camel Main to setup and run Camel
    private static Main main = new Main();
    
    private MessagePublisherClient() { }

    public static void main(String[] args) throws Exception {

        LOG.info("About to run Camel Vertx Kafka integration...");

        String testKafkaMessage = "Test Message from  MessagePublisherClient " + Calendar.getInstance().getTime();

        // add routes
        main.configure().addRoutesBuilder(new RouteBuilder() {
            @Override public void configure() throws Exception {

                // setup kafka component with the brokers using component DSL
                ComponentsBuilderFactory.vertxKafka()
                        .bootstrapServers("{{kafka.host}}:{{kafka.port}}")
                        .register(main.getCamelContext(), "vertx-kafka");

                from("direct:kafkaStart").routeId("DirectToKafka")
                        .to("vertx-kafka:{{producer.topic}}").log("${headers}");

                // Topic can be set in header as well.

                from("direct:kafkaStartNoTopic").routeId("kafkaStartNoTopic")
                        .to("vertx-kafka:dummy")
                        .log("${headers}");

                // Use custom partitioner based on the key.

                from("direct:kafkaStartWithPartitioner").routeId("kafkaStartWithPartitioner")
                        .to("vertx-kafka:{{producer.topic}}?partitionerClass={{producer.partitioner}}")
                        .log("${headers}");


                // Takes input from the command line.

                from("stream:in").setHeader(VertxKafkaConstants.PARTITION_ID, simple("0"))
                        .setHeader(VertxKafkaConstants.MESSAGE_KEY, simple("1")).to("direct:kafkaStart");
            }
        });

        // start and run Camel (block)
        main.run();

        ProducerTemplate producerTemplate = main.getCamelContext().createProducerTemplate();

        Map<String, Object> headers = new HashMap<>();

        headers.put(VertxKafkaConstants.PARTITION_ID, 0);
        headers.put(VertxKafkaConstants.MESSAGE_KEY, "1");
        producerTemplate.sendBodyAndHeaders("direct:kafkaStart", testKafkaMessage, headers);

        // Send with topicName in header

        testKafkaMessage = "TOPIC " + testKafkaMessage;
        headers.put(VertxKafkaConstants.MESSAGE_KEY, "2");
        headers.put(VertxKafkaConstants.TOPIC, "TestLog");

        producerTemplate.sendBodyAndHeaders("direct:kafkaStartNoTopic", testKafkaMessage, headers);

        testKafkaMessage = "PART 0 :  " + testKafkaMessage;
        Map<String, Object> newHeader = new HashMap<>();
        newHeader.put(VertxKafkaConstants.MESSAGE_KEY, "AB"); // This should go to partition 0

        producerTemplate.sendBodyAndHeaders("direct:kafkaStartWithPartitioner", testKafkaMessage, newHeader);

        testKafkaMessage = "PART 1 :  " + testKafkaMessage;
        newHeader.put(VertxKafkaConstants.MESSAGE_KEY, "ABC"); // This should go to partition 1

        producerTemplate.sendBodyAndHeaders("direct:kafkaStartWithPartitioner", testKafkaMessage, newHeader);

        LOG.info("Successfully published event to Kafka.");
        System.out.println("Enter text on the line below : [Press Ctrl-C to exit.] ");

        Thread.sleep(5 * 60 * 1000);

        main.stop();
    }
}
