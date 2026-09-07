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
package org.apache.camel.example.spiffe.policy;

import java.util.Arrays;
import java.util.Optional;

import org.apache.camel.CamelContext;

/**
 * Decides which callers may use which route. Authentication is left to SPIFFE, so this is all the authorization logic
 * the services need: an allow-list of SPIFFE IDs per route, read from the configuration as
 * {@code <service>.allow.<route id>}. A route without an allow-list accepts nobody.
 */
public class AllowList {

    private final CamelContext camelContext;
    private final String prefix;

    public AllowList(CamelContext camelContext, String service) {
        this.camelContext = camelContext;
        this.prefix = service + ".allow.";
    }

    public boolean isAllowed(String routeId, String spiffeId) {
        if (routeId == null || spiffeId == null) {
            return false;
        }
        Optional<String> allowedCallers = camelContext.getPropertiesComponent().resolveProperty(prefix + routeId);
        return allowedCallers.stream()
                .flatMap(callers -> Arrays.stream(callers.split(",")))
                .map(String::trim)
                .anyMatch(spiffeId::equals);
    }
}
