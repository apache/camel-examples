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

import java.util.concurrent.TimeUnit;

import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that the Widget and Gadget use-case from the Enterprise Integration Patterns book works
 * properly using Apache ActiveMQ.
 */
@CamelSpringTest
@ContextConfiguration(locations = {"/META-INF/spring/widget.xml", "/broker.xml"})
class WidgetGadgetTest {

    @Autowired
    ModelCamelContext context;

    @Test
    void should_distribute_orders() throws Exception {
        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(1).wereSentTo("activemq:queue:widget")
                .and().whenCompleted(1).wereSentTo("activemq:queue:gadget")
                .create();

        assertTrue(
                notify.matches(10, TimeUnit.SECONDS),
                "One order should be distributed to widget and and the other to gadget"
        );
    }
}
