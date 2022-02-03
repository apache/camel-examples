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
package org.apache.camel.example.debezium;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws2.kinesis.Kinesis2Constants;
import org.apache.camel.component.debezium.DebeziumConstants;
import org.apache.camel.main.Main;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.kafka.connect.data.Struct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple example to consume data from Debezium and send it to Kinesis
 */
public final class DebeziumPgSQLConsumerToKinesis {

    private static final Logger LOG = LoggerFactory.getLogger(DebeziumPgSQLConsumerToKinesis.class);

    // use Camel Main to set up and run Camel
    private static final Main MAIN = new Main();

    private DebeziumPgSQLConsumerToKinesis() {
    }

    public static void main(String[] args) throws Exception {

        LOG.debug("About to run Debezium integration...");

        // add route
        MAIN.configure().addRoutesBuilder(createRouteBuilder());

        // start and run Camel (block)
        MAIN.run();
    }

    static RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                // Initial Debezium route that will run and listen to the changes,
                // first it will perform an initial snapshot using (select * from) in case no offset
                // exists for the connector, and then it will listen to postgres for any DB events such as (UPDATE, INSERT and DELETE)
                from("debezium-postgres:{{debezium.postgres.name}}?"
                        + "databaseHostname={{debezium.postgres.databaseHostName}}"
                        + "&databasePort={{debezium.postgres.databasePort}}"
                        + "&databaseUser={{debezium.postgres.databaseUser}}"
                        + "&databasePassword={{debezium.postgres.databasePassword}}"
                        + "&databaseServerName={{debezium.postgres.databaseServerName}}"
                        + "&databaseDbname={{debezium.postgres.databaseDbname}}"
                        + "&schemaIncludeList={{debezium.postgres.schemaIncludeList}}"
                        + "&tableIncludeList={{debezium.postgres.tableIncludeList}}"
                        + "&offsetStorageFileName={{debezium.postgres.offsetStorageFileName}}")
                        .routeId("FromDebeziumPgSql")
                        // We will need to prepare the data for Kinesis, however we need to mention here is that Kinesis is a bit different from Kafka in terms
                        // of the key partition which only limited to 256 byte length, depending on the size of your key, that may not be optimal. Therefore, the safer option is to hash the key
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
                            // We will put everything as nested Map in order to utilize Camel's Type Format
                            final Map<String, Object> kinesisBody = new HashMap<>();

                            kinesisBody.put("key", key);
                            kinesisBody.put("value", value);
                            kinesisBody.put("operation", operation);

                            return kinesisBody;
                        })
                        // As we mentioned above, we will need to hash the key partition and set it into the headers
                        .process(exchange -> {
                            final Struct key = (Struct) exchange.getMessage().getHeader(DebeziumConstants.HEADER_KEY);
                            final String hash = String.valueOf(key.hashCode());

                            exchange.getMessage().setHeader(Kinesis2Constants.PARTITION_KEY, hash);
                        })
                        // Marshal everything to JSON, you can use any other data format such as Avro, Protobuf..etc, but in this example we will keep it to JSON for simplicity
                        .marshal().json(JsonLibrary.Jackson)
                        // Send our data to kinesis
                        .to("aws2-kinesis:{{kinesis.streamName}}?accessKey=RAW({{kinesis.accessKey}})"
                                + "&secretKey=RAW({{kinesis.secretKey}})"
                                + "&region={{kinesis.region}}")
                        .end();
            }
        };
    }

}
