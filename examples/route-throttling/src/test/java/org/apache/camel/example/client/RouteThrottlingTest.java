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
package org.apache.camel.example.client;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test allowing to check that the route throttling works as expected.
 */
@CamelSpringTest
@ContextConfiguration("/META-INF/spring/camel-server.xml")
class RouteThrottlingTest {

    @Autowired
    ProducerTemplate template;
    @Autowired
    ModelCamelContext context;

    @Test
    void should_support_route_throttling() {
        int totalMessages = 1_000;
        NotifyBuilder notify = new NotifyBuilder(context).wereSentTo("log:+++SEDA+++*")
                .whenCompleted(totalMessages).create();
        List<CompletableFuture<Void>> futures = IntStream.range(0, totalMessages)
                .mapToObj(i -> CompletableFuture.runAsync(() -> sendMessage(i)))
                .collect(Collectors.toUnmodifiableList());

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        assertTrue(notify.matches(20, TimeUnit.SECONDS), "100 messages should be completed");
    }

    private void sendMessage(int i) {
        if (i % 2 == 0) {
            template.sendBodyAndHeader("file:target//inbox", "File " + i, Exchange.FILE_NAME, i + ".txt");
        } else {
            template.sendBody("jms:queue:inbox", "Message " + i);
        }
    }
}
