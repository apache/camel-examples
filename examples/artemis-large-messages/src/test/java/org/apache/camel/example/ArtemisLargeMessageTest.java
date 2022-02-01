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

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test allowing to ensure that Camel and Artemis can both support large files that cannot fit
 * into the memory.
 */
@CamelSpringTest
@ContextConfiguration("/META-INF/spring/camel-context.xml")
class ArtemisLargeMessageTest {

    @Autowired
    ModelCamelContext context;

    private static ActiveMQServer SERVER;

    @BeforeAll
    static void init() throws Exception {
        Configuration config = new ConfigurationImpl();
        config.addAcceptorConfiguration("tcp", "tcp://localhost:61616");
        config.setJournalDirectory("target/artemis-data/journal");
        config.setBindingsDirectory("target/artemis-data/bindings");
        config.setLargeMessagesDirectory("target/artemis-data/largemessages");
        config.setSecurityEnabled(false);
        SERVER = new ActiveMQServerImpl(config);
        SERVER.start();
    }

    @AfterAll
    static void destroy() throws Exception {
        if (SERVER != null) {
            SERVER.stop();
        }
    }

    @Test
    void should_aggregate_provided_numbers() throws Exception {
        String fileName = UUID.randomUUID().toString();
        // Generate a big file of 1 Go
        try (RandomAccessFile f = new RandomAccessFile(String.format("target/inbox/%s", fileName), "rw")) {
            f.setLength(1024 * 1024 * 1024);
        }

        NotifyBuilder notify = new NotifyBuilder(context).from("jms:queue:data").whenCompleted(1).create();

        assertTrue(
            notify.matches(40, TimeUnit.SECONDS), "The big file should be processed with success"
        );

        assertTrue(
            new File(String.format("target/outbox/%s", fileName)).exists(),
            "The big file should be available in the target/outbox directory"
        );
    }
}
