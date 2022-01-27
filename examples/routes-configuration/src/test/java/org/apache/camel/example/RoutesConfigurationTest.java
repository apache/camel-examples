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

import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.spi.PackageScanResourceResolver;
import org.apache.camel.spi.Resource;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel can load routes dynamically.
 */
class RoutesConfigurationTest extends CamelTestSupport {

    @Test
    void should_load_routes_dynamically() {
        NotifyBuilder notify = new NotifyBuilder(context)
            .from("timer:xml*").whenCompleted(1)
            .and().from("timer:java*").whenCompleted(1)
            .and().from("timer:yaml*").whenCompleted(1).create();
        assertTrue(
            notify.matches(20, TimeUnit.SECONDS), "3 messages should be completed"
        );
    }

    @Override
    protected RoutesBuilder[] createRouteBuilders() throws Exception {
        final ExtendedCamelContext ecc = context.adapt(ExtendedCamelContext.class);
        final PackageScanResourceResolver resolver = ecc.getPackageScanResourceResolver();
        final List<RoutesBuilder> routesBuilders = new ArrayList<>();
        routesBuilders.add(new MyJavaRouteBuilder());
        routesBuilders.add(new MyJavaErrorHandler());
        for (String location : List.of("myroutes/*","myerror/*")) {
            for (Resource resource : resolver.findResources(location)) {
                routesBuilders.addAll(ecc.getRoutesLoader().findRoutesBuilders(resource));
            }
        }
        return routesBuilders.toArray(RoutesBuilder[]::new);
    }
}
