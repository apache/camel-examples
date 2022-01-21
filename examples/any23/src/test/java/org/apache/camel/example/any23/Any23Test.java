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
package org.apache.camel.example.any23;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

/**
 * A unit test checking that we can extract data from a page with RDF content using Apache Anything To Triples.
 */
class Any23Test extends CamelTestSupport {

    @Test
    void should_extract_data_from_rdf() throws Exception {
        // Mock the endpoint log:result
        AdviceWith.adviceWith(context, "result", ad -> ad.mockEndpoints("log:result"));

        // must start Camel after we are done using advice-with
        context.start();

        getMockEndpoint("mock:log:result").expectedMinimumMessageCount(1);

        template.sendBody("direct:start", "This is a test message to run the example");

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new Any23RouteBuilder();
    }
}
