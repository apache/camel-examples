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
package org.apache.camel.example.pulsar.client;

import java.util.Random;

import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Client that uses the {@link ProducerTemplate} to easily exchange messages with the Server.
 * <p/>
 * Requires that the Pulsar broker is running, as well as CamelServer
 */
public final class CamelClient {

    public static final String ENDPOINT_URI = "pulsar:non-persistent://tn1/ns1/cameltest?producerName=clientProd";

    private CamelClient() {
        // Helper class
    }

    public static void main(final String[] args) throws Exception {
        System.out.println("Notice this client requires that the CamelServer is already running!");

        try (AbstractApplicationContext context = new ClassPathXmlApplicationContext("camel-client.xml")) {
            // get the camel template for Spring template style sending of messages (= producer)
            ProducerTemplate camelTemplate = context.getBean("camelTemplate", ProducerTemplate.class);

            for (int i=0; i<10;i++) {
                int input = new Random().nextInt(100);
                System.out.printf("Invoking the multiply with %d%n", input);
                camelTemplate.sendBody(ENDPOINT_URI, ExchangePattern.InOnly, input);
            }
        }
    }

}
