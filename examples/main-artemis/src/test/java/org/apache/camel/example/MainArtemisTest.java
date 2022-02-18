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

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.main.junit5.CamelMainTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.apache.camel.util.PropertiesHelper.asProperties;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel can send and consume messages to/from Apache ActiveMQ Artemis.
 */
class MainArtemisTest extends CamelMainTestSupport {

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

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        return asProperties("myPeriod", Integer.toString(500));
    }

    @Override
    protected void bindToRegistry(Registry registry) throws Exception {
        // Sets up the Artemis core protocol connection factory
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");

        JmsConfiguration configuration = new JmsConfiguration();
        configuration.setConnectionFactory(connectionFactory);

        // connect to ActiveMQ Artemis JMS broker
        registry.bind("jms", new JmsComponent(configuration));
    }

    @Test
    void should_distribute_orders() {

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(1).wereSentTo("jms:queue:cheese")
                .and().whenCompleted(1).from("jms:queue:gadget")
                .create();

        assertTrue(
            notify.matches(20, TimeUnit.SECONDS), "1 message should be exchanged"
        );
    }

    @Override
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addConfiguration(MyConfiguration.class);
        configuration.addRoutesBuilder(MyRouteBuilder.class);
    }
}
