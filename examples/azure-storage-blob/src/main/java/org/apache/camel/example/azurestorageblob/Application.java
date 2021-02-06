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
package org.apache.camel.example.azurestorageblob;

import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.common.StorageSharedKeyCredential;
import java.util.Iterator;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

/**
 * A basic example of list large azure blob storage.
 */
public final class Application {
    private static final String ACCOUNT = "ENTER_YOUR_ACCOUNT";
    private static final String ACCESS_KEY = "ACCESS_KEY";
    private static final String BLOB_CONTAINER_NAME = "CONTAINER";

    public static void main(String[] args) throws Exception {
        // create a CamelContext
        try (CamelContext camel = new DefaultCamelContext()) {

            // add routes which can be inlined as anonymous inner class
            // (to keep all code in a single java file for this basic example)
            camel.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("timer://runOnce?repeatCount=1&delay=0")
                        .routeId("listBlobs")
                        .process(exchange -> exchange.getIn()
                            .setBody(
                                new BlobServiceClientBuilder()
                                    .endpoint(String.format("https://%s.blob.core.windows.net", ACCOUNT))
                                    .credential(new StorageSharedKeyCredential(ACCOUNT, ACCESS_KEY))
                                    .buildClient()
                                    .getBlobContainerClient(BLOB_CONTAINER_NAME)
                                    .listBlobs(
                                        new ListBlobsOptions().setMaxResultsPerPage(1),
                                        null
                                    )
                            )
                        )
                        .loopDoWhile(exchange ->
                            exchange.getIn().getBody(Iterator.class).hasNext()
                        )
                        .process(exchange ->
                            exchange.getIn().setBody(exchange.getIn().getBody(Iterator.class).next())
                        )
                        .log("${body.name}")
                        .end();
                }
            });

            // start is not blocking
            camel.start();

            // so run for 10 seconds
            Thread.sleep(10_000);

            // and then stop nicely
            camel.stop();
        }
    }
}
