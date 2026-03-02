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
package org.apache.camel.example.java8;

import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.test.main.junit6.CamelMainTestSupport;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel supports properly lambda expressions and method references.
 */
class Java8Test extends CamelMainTestSupport {

    @Test
    void should_be_evaluated() {
        NotifyBuilder notify = new NotifyBuilder(context).from("timer:*").whenCompleted(5).create();

        assertTrue(
            notify.matches(5, TimeUnit.SECONDS), "5 messages should be completed"
        );
    }

    @Override
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addRoutesBuilder(new MyRouteBuilder());
    }
}
