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
package org.apache.camel.example.transformer.demo.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.example.transformer.demo.Order;
import org.apache.camel.example.transformer.demo.OrderResponse;
import org.apache.camel.spi.DataType;
import org.apache.camel.spi.DataTypeAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Client that sends order data in various format and verify XML output.
 * <p/>
 */
public final class CamelClient {
    private static final Logger LOG = LoggerFactory.getLogger(CamelClient.class);
    private static final String CSV_PATH = "target/output/orders.csv";
    private CamelClient() {
        // Helper class
    }

    public static void main(final String[] args) throws Exception {
        File csvLogFile = new File(CSV_PATH);
        if (csvLogFile.exists()) {
            LOG.info("---> Removing log file '{}'...", csvLogFile.getAbsolutePath());
            csvLogFile.delete();
        }
        
        // START SNIPPET: e1
        LOG.info("---> Starting 'order' camel route...");
        try (ConfigurableApplicationContext context = new ClassPathXmlApplicationContext("META-INF/spring/camel-context.xml")) {
            context.start();
            CamelContext camelContext = context.getBean("order", CamelContext.class);
            try (ProducerTemplate producer = camelContext.createProducerTemplate()) {
                // END SNIPPET: e1
                Thread.sleep(1000);

                Order order = new Order()
                        .setOrderId("Order-Java-0001")
                        .setItemId("MILK")
                        .setQuantity(3);
                LOG.info("---> Sending '{}' to 'direct:java'", order);
                OrderResponse response = producer.requestBody("direct:java", order, OrderResponse.class);
                logResponse(response);

                String orderXml = "<order orderId=\"Order-XML-0001\" itemId=\"MIKAN\" quantity=\"365\"/>";
                LOG.info("---> Sending '{}' to 'direct:xml'", orderXml);
                Exchange answerXml = producer.send("direct:xml",
                    ex -> ((DataTypeAware) ex.getIn()).setBody(orderXml, new DataType("xml:XMLOrder"))
                );
                logResponse(answerXml.getIn().getBody(String.class));

                String orderJson = "{\"orderId\":\"Order-JSON-0001\", \"itemId\":\"MIZUYO-KAN\", \"quantity\":\"16350\"}";
                LOG.info("---> Sending '{}' to 'direct:json'", orderJson);
                Exchange answerJson = producer.send("direct:json", ex -> ((DataTypeAware) ex.getIn()).setBody(orderJson, new DataType("json")));
                logResponse(answerJson.getIn().getBody(String.class));
            }
        }
    }

    private static void logResponse(Object response) throws Exception {
        Thread.sleep(1000);
        LOG.info("---> Received '{}'", response);
        LOG.info("---> CSV log now contains:\n{}", getCsvLog());
        Thread.sleep(1000);
    }

    public static String getCsvLog() throws IOException {
        return Files.readString(Paths.get(CSV_PATH));
    }
}
