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
package org.apache.camel.example.aitools.assistant;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.example.aitools.policy.ToolAuthorizationPolicy;

/**
 * The work behind each tool, as an ordinary Camel route. The language model does not reach these directly: the
 * {@code langchain4j-tools} bindings in {@link AssistantRoutes} expose them to the model and delegate here, so that the
 * tool logic and its authorization live in a plain route that can be exercised on its own (see the tests).
 * <p>
 * Every route opts in to the {@link ToolAuthorizationPolicy}, which authorizes the call in-process against the
 * WebAssembly policy before the route's own steps run. The route id is the tool name the policy decides on.
 */
public class ToolRoutes extends RouteBuilder {

    private final RefundLedger refundLedger;

    public ToolRoutes(RefundLedger refundLedger) {
        this.refundLedger = refundLedger;
    }

    @Override
    public void configure() {
        // read the status of an order: low risk, allowed to every authenticated caller
        from("direct:getOrderStatus").routeId("getOrderStatus")
                .routeConfigurationId(ToolAuthorizationPolicy.ID)
                .bean(OrderService.class, "status");

        // look up the customer behind an order: returns personal data, so only the support console may call it
        from("direct:lookupCustomer").routeId("lookupCustomer")
                .routeConfigurationId(ToolAuthorizationPolicy.ID)
                .bean(CustomerService.class, "lookup");

        // refund an order: moves money, so only the support console may call it, and only up to the policy's cap
        from("direct:refundOrder").routeId("refundOrder")
                .routeConfigurationId(ToolAuthorizationPolicy.ID)
                .bean(refundLedger, "refund");
    }
}
