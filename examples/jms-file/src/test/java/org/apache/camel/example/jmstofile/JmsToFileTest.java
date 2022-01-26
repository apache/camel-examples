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
package org.apache.camel.example.jmstofile;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.apache.camel.example.jmstofile.CamelJmsToFileExample.createActiveMQConnectionFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel can read messages from a JMS queue and store them into the file system as files.
 */
class JmsToFileTest extends CamelTestSupport {

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        // Set up the ActiveMQ JMS Components
        // tag::e2[]
        ActiveMQConnectionFactory connectionFactory = createActiveMQConnectionFactory();
        // Note we can explicitly name the component
        camelContext.addComponent("test-jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

        return camelContext;
    }

    @Test
    void should_store_jms_messages_as_files() throws IOException {
        Path targetDir = Paths.get("target/messages");
        long before = Files.exists(targetDir) ? Files.list(targetDir).count() : 0L;
        NotifyBuilder notify = new NotifyBuilder(context).from("test-jms:*").whenCompleted(5).create();
        for (int i = 0; i < 5; i++) {
            template.sendBody("test-jms:queue:test.queue", "Test Message: " + i);
        }
        assertTrue(
            notify.matches(5, TimeUnit.SECONDS), "5 messages should be completed"
        );
        long after = Files.list(targetDir).count();
        assertEquals(5L, after - before, "5 files should have been created");
    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new CamelJmsToFileExample.MyRouteBuilder();
    }
}
