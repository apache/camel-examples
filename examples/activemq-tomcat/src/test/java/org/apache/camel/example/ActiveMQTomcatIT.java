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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A basic integration test allowing to ensure that the messages are properly sent
 * to ActiveMQ by the routes executed in Tomcat.
 */
@ExtendWith(ArquillianExtension.class)
public class ActiveMQTomcatIT {

    @Deployment
    public static WebArchive createDeployment() {
        // build the .war with all the resources and libraries
        return ShrinkWrap.create(WebArchive.class, "camel-example-activemq-tomcat.war")
            .setWebXML(new File("./src/main/webapp/WEB-INF/web.xml"))
            .addAsResource("log4j2.properties")
            .addAsResource("broker.xml")
            .addAsResource("camel-config.xml")
            .addAsResource("test-application.properties", "application.properties")
            .addAsLibraries(
                Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve()
                    .withTransitivity().asFile()
            );
    }

    @Test
    void should_consume_jms_messages() throws Exception {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://myBroker");
        // Create a Connection
        try (Connection connection = connectionFactory.createConnection()) {
            connection.start();
            // Create a Session and a consumer
            try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                 MessageConsumer consumer = session.createConsumer(session.createQueue("outbox"))) {
                // Wait for a message
                assertNotNull(consumer.receive(10_000)) ;
            }
        }
    }
}
