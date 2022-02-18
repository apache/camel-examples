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

import java.io.File;
import java.io.FileOutputStream;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Producer;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringJmsClientServerTest extends CamelSpringTestSupport {

    @BeforeAll
    public static void setupFreePort() throws Exception {
        // find a free port number, and write that in the custom.properties file
        // which we will use for the unit tests, to avoid port number in use problems
        int port = AvailablePortFinder.getNextAvailable();
        String bank1 = "tcp.port=" + port;

        File custom = new File("target/custom.properties");
        FileOutputStream fos = new FileOutputStream(custom);
        fos.write(bank1.getBytes());
        fos.close();
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/META-INF/spring/camel-server.xml");
    }
    
    @Test
    void testCamelClientInvocation() {
        // as opposed to the CamelClientRemoting example we need to define the service URI in this java code
        int response = template.requestBody("jms:queue:numbers", 22, Integer.class);
        assertEquals(66, response, "Get a wrong response");
    }
    
    @Test
    void testCamelEndpointInvocation() throws Exception {
        // get the endpoint from the camel context
        Endpoint endpoint = context.getEndpoint("jms:queue:numbers");

        // create the exchange used for the communication
        // we use the in out pattern for a synchronized exchange where we expect a response
        Exchange exchange = endpoint.createExchange(ExchangePattern.InOut);
        // set the input on the in body
        // must you correct type to match the expected type of an Integer object
        exchange.getIn().setBody(11);

        // to send the exchange we need an producer to do it for us
        Producer producer = endpoint.createProducer();
        // start the producer so it can operate
        producer.start();

        // let the producer process the exchange where it does all the work in this one line of code
        producer.process(exchange);

        // get the response from the out body and cast it to an integer
        int response = exchange.getIn().getBody(Integer.class);
        
        assertEquals(33, response, "Get a wrong response.");

        // stop the producer after usage
        producer.stop();
    }
    
}
