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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckResolver;

public class MyRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // to trigger the health check to flip between UP and DOWN then lets call it as a Java bean from
        // a route, and therefore we need to lookup and resolve the monkey-check
        HealthCheckResolver resolver = getCamelContext().adapt(ExtendedCamelContext.class).getHealthCheckResolver();
        final HealthCheck monkey = resolver.resolveHealthCheck("monkey");

        from("timer:foo?period={{myPeriod}}").routeId("timer")
            .bean(monkey, "chaos")
            .log("${body}");

        // this route is invalid and fails during startup
        // the supervising route controller will take over and attempt
        // to restart this route
        from("netty:tcp:unknownhost").to("log:dummy").routeId("netty");
    }
}
