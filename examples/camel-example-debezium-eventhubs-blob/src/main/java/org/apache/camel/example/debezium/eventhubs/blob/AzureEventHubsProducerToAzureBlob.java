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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple example to sink data from Azure Event Hubs that produced by Debezium into Azure Storage Blob
 */
public final class AzureEventHubsProducerToAzureBlob {

    private static final Logger LOG = LoggerFactory.getLogger(AzureEventHubsProducerToAzureBlob.class);

    // use Camel Main to setup and run Camel
    private static Main main = new Main();

    private AzureEventHubsProducerToAzureBlob() {
    }

    public static void main(String[] args) throws Exception {

        LOG.debug("About to run Event Hubs to Storage Blob integration..");

        // add route
        main.configure().addRoutesBuilder(new RouteBuilder() {
            public void configure() {
                from("azure-eventhubs:?connectionString=RAW({{eventhubs.connectionString}})"
                        + "&blobContainerName={{blob.containerName}}"
                        + "&blobAccountName={{blob.accountName}}"
                        + "&blobAccessKey=RAW({{blob.accessKey}})")
                        // write our data to Azure Blob Storage but committing to an existing append blob
                        .to("azure-storage-blob://{{blob.accountName}}/{{blob.containerName}}?operation=commitAppendBlob"
                                + "&accessKey=RAW({{blob.accessKey}})"
                                + "&blobName={{blob.blobName}}")
                        .end();
            }
        });

        // start and run Camel (block)
        main.run();
    }

}
