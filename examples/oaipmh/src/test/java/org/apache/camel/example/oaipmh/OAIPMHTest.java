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
package org.apache.camel.example.oaipmh;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel can extract data using OAI-PMH.
 */
class OAIPMHTest extends CamelTestSupport {

    @Test
    void should_extract_title_publications() {
        NotifyBuilder notify = new NotifyBuilder(context).wereSentTo("log:titles").whenCompleted(1).create();
        assertTrue(
            notify.matches(30, TimeUnit.SECONDS), "at least 1 message should be completed"
        );
    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new OAIPMHRouteBuilder();
    }
}
