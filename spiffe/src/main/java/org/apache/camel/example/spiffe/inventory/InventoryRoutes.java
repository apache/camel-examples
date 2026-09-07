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
package org.apache.camel.example.spiffe.inventory;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.example.spiffe.policy.WorkloadIdentityPolicy;

/**
 * The inventory answers the stock levels to the backend, and to nobody else.
 */
public class InventoryRoutes extends RouteBuilder {

    @Override
    public void configure() {
        from("platform-http:/api/stock?httpMethodRestrict=GET").routeId("stock")
                .routeConfigurationId(WorkloadIdentityPolicy.ID)
                // the policy has checked that the caller is the backend. The backend says on whose behalf it asks,
                // which can be trusted because the backend itself is authenticated and allowed to call this route
                .log("Serving the stock levels to ${header.CamelSpiffeSpiffeId} on behalf of ${header.X-On-Behalf-Of}")
                .bean(StockService.class, "levels")
                .marshal().json()
                .removeHeaders("CamelSpiffe*");
    }
}
