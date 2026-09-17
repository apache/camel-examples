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

/**
 * Exposes the assistant's tools to the language model with the {@code ai-tool} component. Each binding registers a
 * Camel route as a tool in the shared tool registry: its description and parameters tell the model what the tool does,
 * and it delegates to the matching work route in {@link ToolRoutes}, where the tool is authorized and run. The agent
 * discovers these tools by the {@code support} tag they share (see {@link AssistantRoutes}).
 * <p>
 * The bindings are kept apart from the HTTP entry point in {@link AssistantRoutes} so the tools, their authorization
 * and the model can be wired up (and tested) without the SPIFFE-authenticated front door.
 */
public class ToolBindings extends RouteBuilder {

    @Override
    public void configure() {
        from("ai-tool:getOrderStatus?tags=support&readOnlyHint=true"
             + "&description=Return the current delivery status of a customer order"
             + "&parameter.orderId=string&parameter.orderId.description=The id of the order, for example 1002")
                .routeId("tool-getOrderStatus")
                .to("direct:getOrderStatus");

        from("ai-tool:lookupCustomer?tags=support&readOnlyHint=true"
             + "&description=Return the customer (name and contact details) behind an order"
             + "&parameter.orderId=string&parameter.orderId.description=The id of the order, for example 1002")
                .routeId("tool-lookupCustomer")
                .to("direct:lookupCustomer");

        from("ai-tool:refundOrder?tags=support&destructiveHint=true"
             + "&description=Refund a customer order by its id, for an amount in dollars"
             + "&parameter.orderId=string&parameter.orderId.description=The id of the order to refund, for example 1002"
             + "&parameter.amount=integer&parameter.amount.description=The amount to refund in dollars, for example 50")
                .routeId("tool-refundOrder")
                .to("direct:refundOrder");
    }
}
