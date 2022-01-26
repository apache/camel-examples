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
package org.apache.camel.example.jmx;

import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test allowing to check that Camel can subscribe to the notifications of a custom MBean.
 */
@CamelSpringTest
@ContextConfiguration("/META-INF/spring/camel-context.xml")
@UseAdviceWith
class JMXTest {

    @Autowired
    ModelCamelContext context;

    @Test
    void should_receive_mbean_notifications() throws Exception {
        // Replace the from endpoint to change the value of the option period
        AdviceWith.adviceWith(context.getRouteDefinitions().get(1), context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                replaceFromWith("timer:foo?period=1000");
            }
        });

        // must start Camel after we are done using advice-with
        context.start();

        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(3).wereSentTo("log:jmxEvent").create();

        assertTrue(
            notify.matches(10, TimeUnit.SECONDS), "3 messages should be completed"
        );
    }
}
