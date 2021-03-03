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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.component.ComponentsBuilderFactory;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MessageConsumerClient {

    private static final Logger LOG = LoggerFactory.getLogger(MessageConsumerClient.class);

    // use Camel Main to setup and run Camel
    private static Main main = new Main();

    private MessageConsumerClient() {
    }

    public static void main(String[] args) throws Exception {

        LOG.info("About to run Camel Vertx Kafka integration...");


        // Add route to send messages to Kafka
        main.configure().addRoutesBuilder(new RouteBuilder() {
            public void configure() {
                log.info("About to start route: Kafka Server -> Log ");

                // setup kafka component with the brokers
                ComponentsBuilderFactory.vertxKafka()
                        .bootstrapServers("{{kafka.host}}:{{kafka.port}}")
                        .register(main.getCamelContext(), "vertx-kafka");

                from("vertx-kafka:{{consumer.topic}}"
                        + "?maxPollRecords={{consumer.maxPollRecords}}"
                        + "&seekToPosition={{consumer.seekTo}}"
                        + "&groupId={{consumer.group}}")
                        .routeId("FromKafka")
                    .log("${body}");
            }
        });
        main.start();
        // let it run for 5 minutes before shutting down
        Thread.sleep(5 * 60 * 1000);

        main.stop();
    }

}
