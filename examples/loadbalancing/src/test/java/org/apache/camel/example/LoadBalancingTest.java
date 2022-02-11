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
package org.apache.camel.example;

import org.apache.camel.Predicate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.example.model.Report;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test allowing to check that Camel can load balance the load.
 */
@CamelSpringTest
@ContextConfiguration(locations = { "test-camel-context.xml" })
class LoadBalancingTest {

    @Autowired
    ModelCamelContext context;

    @Test
    void should_support_load_balancing() {
        final String[] minaServers = {"localhost:9991", "localhost:9992"};
        final Predicate predicate = exchange -> {
            final Report report = exchange.getIn().getBody(Report.class);
            return report.getReply().contains(minaServers[(report.getId() - 1) % 2]);
        };
        NotifyBuilder notify = new NotifyBuilder(context).fromRoute("sendMessage").whenCompleted(4)
                .whenAllDoneMatches(predicate)
                .create();
        assertTrue(
            notify.matches(20, TimeUnit.SECONDS), "4 messages should be completed"
        );
    }
}
