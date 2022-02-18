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
package org.apache.camel.loanbroker;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoanBrokerQueueTest extends CamelTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(LoanBrokerQueueTest.class);

    private AbstractApplicationContext applicationContext;
    private CamelContext camelContext;
    private ProducerTemplate template;

    @BeforeEach
    public void startServices() throws Exception {
        applicationContext = new ClassPathXmlApplicationContext("META-INF/spring/server.xml");
        camelContext = getCamelContext();
        camelContext.addRoutes(new LoanBrokerRoute());
        template = camelContext.createProducerTemplate();
        camelContext.start();
    }

    @AfterEach
    public void stopServices() throws Exception {
        if (camelContext != null) {
            camelContext.stop();
        }
    }

    @Test
    void testClientInvocation() throws Exception {
        String out = template.requestBodyAndHeader("jms:queue:loan", null, Constants.PROPERTY_SSN, "Client-A", String.class);

        LOG.info("Result: {}", out);
        assertNotNull(out);
        assertTrue(out.startsWith("The best rate is [ssn:Client-A bank:bank"));
    }

    protected CamelContext getCamelContext() throws Exception {
        return applicationContext.getBean("camel", CamelContext.class);
    }

}
