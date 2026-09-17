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
package org.apache.camel.example.aitools;

import java.util.List;
import java.util.Map;

import org.apache.camel.example.aitools.assistant.RefundLedger;
import org.apache.camel.example.aitools.assistant.ToolRoutes;
import org.apache.camel.example.aitools.policy.ToolAuthorizationPolicy;
import org.apache.camel.example.aitools.policy.ToolCallAudit;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.test.main.junit6.CamelMainTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the in-process authorization guard directly, without a language model. Each test sends an exchange to a
 * tool's work route with a caller identity (the {@code subject} property, as the assistant sets it from a validated
 * JWT-SVID) and the tool arguments (headers, as the model would fill them in), and checks the decision.
 * <p>
 * This is the real thing: the {@link ToolAuthorizationPolicy} evaluates the actual WebAssembly bundle
 * ({@code opa/tools-bundle.tar.gz}, built from {@code opa/tools.rego}) in-process with the camel-opa component. No OPA
 * server and no SPIRE agent are involved, so the whole matrix runs offline.
 */
class OpaWasmToolGuardTest extends CamelMainTestSupport {

    private static final String PUBLIC_CHATBOT = "spiffe://example.org/public-chatbot";
    private static final String SUPPORT_CONSOLE = "spiffe://example.org/support-console";

    private final RefundLedger refundLedger = new RefundLedger();

    @Override
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addRoutesBuilder(new ToolAuthorizationPolicy(new ToolCallAudit()));
        configuration.addRoutesBuilder(new ToolRoutes(refundLedger));
    }

    @Test
    void publicChatbotMayGetOrderStatus() {
        String result = callTool("direct:getOrderStatus", PUBLIC_CHATBOT, Map.of("orderId", "1002"));
        assertTrue(result.contains("order 1002"), result);
    }

    @Test
    void publicChatbotMayNotLookUpCustomer() {
        String result = callTool("direct:lookupCustomer", PUBLIC_CHATBOT, Map.of("orderId", "1002"));
        assertTrue(result.startsWith("Access denied"), result);
    }

    @Test
    void publicChatbotMayNotRefund() {
        // the prompt-injection case: the model was talked into calling refundOrder for the public chatbot
        String result = callTool("direct:refundOrder", PUBLIC_CHATBOT, Map.of("orderId", "1002", "amount", "5"));
        assertTrue(result.startsWith("Access denied"), result);
        assertTrue(refundLedger.refunds().isEmpty(), "no refund must have been recorded");
    }

    @Test
    void supportConsoleMayLookUpCustomer() {
        String result = callTool("direct:lookupCustomer", SUPPORT_CONSOLE, Map.of("orderId", "1002"));
        assertTrue(result.contains("Fox Mulder"), result);
    }

    @Test
    void supportConsoleMayRefundWithinTheCap() {
        String result = callTool("direct:refundOrder", SUPPORT_CONSOLE, Map.of("orderId", "1002", "amount", "50"));
        assertEquals("refunded 50 dollars on order 1002", result);
        assertEquals(List.of("1002:50"), refundLedger.refunds());
    }

    @Test
    void supportConsoleMayNotRefundAboveTheCap() {
        String result = callTool("direct:refundOrder", SUPPORT_CONSOLE, Map.of("orderId", "1002", "amount", "500"));
        assertTrue(result.startsWith("Access denied"), result);
        assertTrue(refundLedger.refunds().isEmpty(), "a refund above the cap must not be recorded");
    }

    @Test
    void unknownCallerIsDenied() {
        String result = callTool("direct:getOrderStatus", "spiffe://example.org/intruder", Map.of("orderId", "1002"));
        assertTrue(result.startsWith("Access denied"), result);
    }

    /** Invokes a tool route as a caller would be seen after authentication: the subject is a property, the tool
     * arguments are headers. Returns the tool result (or the policy's refusal). */
    private String callTool(String toolRoute, String subject, Map<String, Object> arguments) {
        return template.request(toolRoute, exchange -> {
            exchange.setProperty("subject", subject);
            arguments.forEach((name, value) -> exchange.getMessage().setHeader(name, value));
        }).getMessage().getBody(String.class);
    }
}
