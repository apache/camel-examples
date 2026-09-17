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
package org.apache.camel.example.aitools.policy;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.apache.camel.component.opa.OpaConstants;
import org.apache.camel.component.opa.OpaPolicyEvaluationException;
import org.apache.camel.model.RouteConfigurationDefinition;

/**
 * The authorization guard shared by every tool the assistant exposes to the language model. It is a route
 * configuration: each tool route opts in with {@code routeConfigurationId(ToolAuthorizationPolicy.ID)}, and this check
 * runs before the tool's own logic, so the tool routes carry business logic only.
 * <p>
 * The decision is taken by Open Policy Agent, but with the {@code camel-opa} component in {@code wasm} mode: the policy
 * is a WebAssembly module compiled from {@code opa/tools.rego} and evaluated <em>in-process</em>, with no OPA server to
 * call. That matters here because a tool call sits in the middle of the model's reasoning loop: the check must be fast
 * and must not add a network hop or a point that can be unreachable.
 * <p>
 * What is sent to the policy is deliberately narrow: the authenticated caller (an exchange <em>property</em>, set from
 * the validated JWT-SVID before the model ran, and therefore not something the model or a prompt-injected instruction
 * can change) and the tool arguments the model filled in (the {@code orderId} and {@code amount} headers). This is the
 * guardrail that keeps an over-eager or manipulated model inside what its caller is actually allowed to do.
 */
public class ToolAuthorizationPolicy extends RouteConfigurationBuilder {

    /** The id with which the tool routes opt in to this policy. */
    public static final String ID = "tool-authorization";

    // Evaluate the WebAssembly bundle in-process. entrypoint is the rule the bundle was built with
    // (opa build -e ai/tools/allow, see build-policy.sh); it happens to match the policy path here, but is spelled out
    // for clarity. The policy is sent the authenticated caller and the tool being called (both properties) and the tool
    // arguments (headers). The tool is the current route id, captured into a property because the input.routeId that
    // camel-opa derives is the route the exchange originated from - here the agent's route, not the tool's.
    private static final String GUARD = "opa:ai/tools/allow"
                                        + "?evaluationMode=wasm"
                                        + "&policyBundle=classpath:opa/tools-bundle.tar.gz"
                                        + "&entrypoint=ai/tools/allow"
                                        + "&includeProperties=subject,tool"
                                        + "&includeHeaders=orderId,amount";

    private final ToolCallAudit audit;

    public ToolAuthorizationPolicy(ToolCallAudit audit) {
        this.audit = audit;
    }

    @Override
    public void configuration() {
        RouteConfigurationDefinition policy = routeConfiguration(ID);

        // the policy could not be evaluated (a malformed input, a broken bundle): fail closed. With the bundle
        // evaluated in-process there is no server to be unreachable, so this should not normally happen.
        policy.onException(OpaPolicyEvaluationException.class)
                .handled(true)
                .bean(audit, "record(${exchangeProperty.subject}, ${routeId}, 'error', ${exception.message})")
                .log(LoggingLevel.ERROR,
                        "Could not authorize the ${routeId} tool for ${exchangeProperty.subject}: ${exception.message}")
                .setBody(simple("The ${routeId} tool is unavailable: its authorization policy could not be evaluated"))
                // the model receives this as the tool result; the tool logic itself does not run
                .stop();

        // runs before the first step of every tool route that uses this configuration
        policy.interceptFrom()
                // the tool being called is this route; capture it so the policy can authorize it (see GUARD above)
                .setProperty("tool", simple("${routeId}"))
                .to(GUARD)
                .choice()
                    .when(header(OpaConstants.DECISION_ALLOW).isEqualTo(true))
                        .bean(audit, "record(${exchangeProperty.subject}, ${routeId}, 'allowed', null)")
                        .log("Allowed ${exchangeProperty.subject} to use the ${routeId} tool")
                        .removeHeaders("CamelOpa*")
                    .otherwise()
                        .bean(audit, "record(${exchangeProperty.subject}, ${routeId}, 'denied', null)")
                        .log(LoggingLevel.WARN,
                                "DENIED ${routeId} for ${exchangeProperty.subject}: the tool was not run")
                        // the tool does not run: the model receives this refusal as the tool result and relays it
                        .setBody(simple("Access denied: the caller is not allowed to use the ${routeId} tool"))
                        .removeHeaders("CamelOpa*")
                        .stop()
                .end();
    }
}
