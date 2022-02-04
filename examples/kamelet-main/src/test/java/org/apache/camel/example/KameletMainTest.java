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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.kamelet.KameletComponent;
import org.apache.camel.spi.PackageScanResourceResolver;
import org.apache.camel.spi.Resource;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel can use a simple Kamelet from the catalog.
 */
class KameletMainTest extends CamelTestSupport {

    @Test
    void should_use_a_simple_kamelet_from_catalog() {
        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(1).create();
        assertTrue(
            notify.matches(20, TimeUnit.SECONDS), "1 message should be completed"
        );
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        KameletComponent component = camelContext.getComponent("kamelet", KameletComponent.class);
        // Indicates where the kamelet should be found
        component.setLocation("github:apache:camel-kamelets/kamelets");
        return camelContext;
    }

    @Override
    protected RoutesBuilder[] createRouteBuilders() throws Exception {
        final ExtendedCamelContext ecc = context.adapt(ExtendedCamelContext.class);
        final PackageScanResourceResolver resolver = ecc.getPackageScanResourceResolver();
        final List<RoutesBuilder> routesBuilders = new ArrayList<>();
        for (Resource resource : resolver.findResources("camel/*")) {
            routesBuilders.addAll(ecc.getRoutesLoader().findRoutesBuilders(resource));
        }
        return routesBuilders.toArray(RoutesBuilder[]::new);
    }
}
