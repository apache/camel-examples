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
package org.apache.camel.example.debezium.eventhubs.blob;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.eventhubs.EventHubsConstants;
import org.apache.camel.component.debezium.DebeziumConstants;
import org.apache.camel.main.Main;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.kafka.connect.data.Struct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple example to consume data from Debezium and send it to Azure EventHubs
 */
public final class DebeziumMySqlConsumerToAzureEventHubs {

    private static final Logger LOG = LoggerFactory.getLogger(DebeziumMySqlConsumerToAzureEventHubs.class);

    // use Camel Main to setup and run Camel
    private static Main main = new Main();

    private DebeziumMySqlConsumerToAzureEventHubs() {
    }

    public static void main(String[] args) throws Exception {

        LOG.debug("About to run Debezium integration...");

        // add route
        main.configure().addRoutesBuilder(new RouteBuilder() {
            public void configure() {
                // Initial Debezium route that will run and listens to the changes,
                // first it will perform an initial snapshot using (select * from) in case there are no offsets
                // exists for the connector, and then it will listen to MySQL binlogs for any DB events such as (UPDATE, INSERT and DELETE)
                from("debezium-mysql:{{debezium.mysql.name}}?"
                        + "databaseServerId={{debezium.mysql.databaseServerId}}"
                        + "&databaseHostname={{debezium.mysql.databaseHostName}}"
                        + "&databasePort={{debezium.mysql.databasePort}}"
                        + "&databaseUser={{debezium.mysql.databaseUser}}"
                        + "&databasePassword={{debezium.mysql.databasePassword}}"
                        + "&databaseServerName={{debezium.mysql.databaseServerName}}"
                        + "&databaseHistoryFileFilename={{debezium.mysql.databaseHistoryFileName}}"
                        + "&databaseIncludeList={{debezium.mysql.databaseIncludeList}}"
                        + "&tableIncludeList={{debezium.mysql.tableIncludeList}}"
                        + "&offsetStorageFileName={{debezium.mysql.offsetStorageFileName}}")
                        .routeId("FromDebeziumMySql")
                        // We will need to prepare the data for Azure EventHubs Therefore, we will hash the key to make sure our record land on the same partition
                        // and convert it to string, but that means we need to preserve the key information into the message body in order not to lose this information downstream.
                        // Note: If you'd use Kafka, most probably you will not need these transformations as you can send the key as an object and Kafka will do
                        // the rest to hash it in the broker in order to place it in the correct topic's partition.
                        .setBody(exchange -> {
                            // Using Camel Data Format, we can retrieve our data in Map since Debezium component has a Type Converter from Struct to Map, you need to specify the Map.class
                            // in order to convert the data from Struct to Map
                            final Map key = exchange.getMessage().getHeader(DebeziumConstants.HEADER_KEY, Map.class);
                            final Map value = exchange.getMessage().getBody(Map.class);
                            // Also, we need the operation in order to determine when an INSERT, UPDATE or DELETE happens
                            final String operation = (String) exchange.getMessage().getHeader(DebeziumConstants.HEADER_OPERATION);
                            // We we will put everything as nested Map in order to utilize Camel's Type Format
                            final Map<String, Object> eventHubBody = new HashMap<>();

                            eventHubBody.put("key", key);
                            eventHubBody.put("value", value);
                            eventHubBody.put("operation", operation);

                            return eventHubBody;
                        })
                        // As we mentioned above, we will need to hash the key partition and set it into the headers
                        .process(exchange -> {
                            final Struct key = (Struct) exchange.getMessage().getHeader(DebeziumConstants.HEADER_KEY);
                            final String hash = String.valueOf(key.hashCode());

                            exchange.getMessage().setHeader(EventHubsConstants.PARTITION_KEY, hash);
                        })
                        // Marshal everything to JSON, you can use any other data format such as Avro, Protobuf..etc, but in this example we will keep it to JSON for simplicity
                        .marshal().json(JsonLibrary.Jackson)
                        // Send our data to Azure Event Hubs
                        .to("azure-eventhubs:?connectionString=RAW({{eventhubs.connectionString}})")
                        .end();
            }
        });

        // start and run Camel (block)
        main.run();
    }

}
