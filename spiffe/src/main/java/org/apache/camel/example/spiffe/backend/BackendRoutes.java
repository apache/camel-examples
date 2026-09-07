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
package org.apache.camel.example.spiffe.backend;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.spiffe.SpiffeConstants;
import org.apache.camel.example.spiffe.policy.AuditTrail;
import org.apache.camel.example.spiffe.policy.WorkloadIdentityPolicy;

/**
 * The backend exposes two HTTP routes, both guarded by the {@link WorkloadIdentityPolicy}: the orders, which are
 * completed with the stock levels of the inventory service (the second hop), and the audit trail of the policy.
 */
public class BackendRoutes extends RouteBuilder {

    private final AuditTrail auditTrail;

    public BackendRoutes(AuditTrail auditTrail) {
        this.auditTrail = auditTrail;
    }

    @Override
    public void configure() {
        // the inventory could not be reached, or refused our identity: the orders cannot be served
        onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.ERROR, "Could not get the stock levels from the inventory: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(502))
                .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
                .setBody(simple("502 Bad Gateway: ${exception.message}"))
                .removeHeaders("CamelSpiffe*");

        from("platform-http:/api/orders?httpMethodRestrict=GET").routeId("orders")
                .routeConfigurationId(WorkloadIdentityPolicy.ID)
                // the policy has authenticated and authorized the caller by the time the route starts
                .bean(OrderService.class, "listOrders")
                .setProperty("orders", body())
                .setProperty("onBehalfOf", header(SpiffeConstants.SPIFFE_ID))
                .to("direct:stockLevels")
                .bean(OrderService.class, "withStock")
                .marshal().json()
                .removeHeaders("CamelSpiffe*");

        // the second hop: ask the inventory service for the stock levels, with our own identity. The JWT-SVID is
        // minted for the inventory (its audience), and the caller we are serving travels along in a header for the
        // audit trail of the inventory, which trusts it because we are authenticated and allowed to call it
        from("direct:stockLevels").routeId("stock-levels")
                .to("spiffe:backend?operation=fetchJwtSvid&audience={{inventory.audience}}")
                .setHeader("Authorization", simple("Bearer ${body}"))
                .setHeader("X-On-Behalf-Of", exchangeProperty("onBehalfOf"))
                .setBody(simple("${null}"))
                // the headers of the request we are serving must not shape the request we are making
                .removeHeaders("CamelHttp*")
                .removeHeaders("CamelSpiffe*")
                .to("http://{{inventory.host}}:{{inventory.port}}/api/stock?httpMethod=GET")
                // our token is not for the caller to see
                .removeHeader("Authorization")
                .removeHeader("X-On-Behalf-Of")
                .unmarshal().json(Map.class);

        from("platform-http:/api/audit?httpMethodRestrict=GET").routeId("audit")
                .routeConfigurationId(WorkloadIdentityPolicy.ID)
                .bean(auditTrail, "report")
                .marshal().json()
                .removeHeaders("CamelSpiffe*");
    }
}
