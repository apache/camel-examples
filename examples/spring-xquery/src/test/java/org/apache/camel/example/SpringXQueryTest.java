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
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * A unit test allowing to check that Camel can transform messages using XQuery.
 */
@CamelSpringTest
@ContextConfiguration("/META-INF/spring/camelContext.xml")
@MockEndpoints("file:target/outputFiles")
class SpringXQueryTest {

    @EndpointInject("mock:file:target/outputFiles")
    MockEndpoint result;

    @Test
    void should_transform_messages_with_xquery() throws Exception {

        result.expectedBodiesReceived("<employee id=\"james\"><name>James Strachan</name><location>London</location></employee>");
        result.expectedMessageCount(1);

        result.assertIsSatisfied();
    }
}
