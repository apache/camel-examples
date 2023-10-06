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

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * A unit test allowing to check that the aggregation works as expected.
 */
@CamelSpringTest
@ContextConfiguration("/META-INF/spring/camel-context.xml")
@MockEndpoints("stream:out")
@UseAdviceWith
class AggregateTest {

    @Autowired
    ProducerTemplate template;
    @Autowired
    ModelCamelContext context;
    @EndpointInject("mock:stream:out")
    MockEndpoint result;

    @Test
    void should_aggregate_provided_numbers() throws Exception {
        // Replace the from endpoint to send messages easily
        AdviceWith.adviceWith(context.getRouteDefinitions().get(0), context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                replaceFromWith("direct:in");
            }
        });

        // must start Camel after we are done using advice-with
        context.start();

        result.expectedBodiesReceived("The result is: 6");
        result.expectedMessageCount(1);

        template.sendBody("direct:in", "1");
        template.sendBody("direct:in", "2");
        template.sendBody("direct:in", "3");
        template.sendBody("direct:in", "STOP");

        result.assertIsSatisfied();
    }
}
