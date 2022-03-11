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
package org.apache.camel.example.debezium.eventhubs.blob.consumer;

import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple example to consume data from Debezium and send it to Azure EventHubs
 */
public final class DebeziumMySqlConsumerToAzureEventHubs {

    private static final Logger LOG = LoggerFactory.getLogger(DebeziumMySqlConsumerToAzureEventHubs.class);

    private DebeziumMySqlConsumerToAzureEventHubs() {
    }

    public static void main(String[] args) throws Exception {
        LOG.debug("About to run Debezium integration...");
        // use Camels Main class
        Main main = new Main(DebeziumMySqlConsumerToAzureEventHubs.class);
        // start and run Camel (block)
        main.run();
    }

}
