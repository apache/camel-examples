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

import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.test.main.junit5.CamelMainTestSupport;
import org.junit.jupiter.api.Test;

import static org.apache.camel.util.PropertiesHelper.asProperties;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel supports custom health-checks.
 */
class MainHealthTest extends CamelMainTestSupport {

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        return asProperties("myPeriod", Integer.toString(500));
    }

    @Override
    public void setupResources() throws Exception {
        // Prevent failure by replacing the failing endpoint
    	// still use the deprecated method for now as method is not visible camelContextConfiguration().replaceRouteFromWith("netty", "direct:foo");
        replaceRouteFromWith("netty", "direct:foo");
        super.setupResources();
    }

    @Test
    void should_support_custom_health_checks() throws Exception {
        NotifyBuilder notify = new NotifyBuilder(context)
            .whenCompleted(2).whenBodiesDone("Chaos monkey was here", "All is okay").create();
        assertTrue(
            notify.matches(20, TimeUnit.SECONDS), "2 messages should be completed"
        );
    }

    @Override
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addRoutesBuilder(MyRouteBuilder.class);
    }
}
