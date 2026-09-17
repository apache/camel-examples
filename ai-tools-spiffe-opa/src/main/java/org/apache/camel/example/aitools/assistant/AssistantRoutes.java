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

import io.spiffe.exception.JwtSvidException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.spiffe.SpiffeConstants;
import org.apache.camel.example.aitools.policy.BearerToken;
import org.apache.camel.example.aitools.policy.RejectionReason;

/**
 * The assistant's HTTP entry point and the tools it offers the language model.
 * <p>
 * A caller (another workload) posts a natural-language message with its JWT-SVID as a bearer token. The assistant
 * authenticates it with SPIFFE, records the caller's SPIFFE ID as an exchange property, and hands the message to the
 * model with the set of tools tagged {@code support}. The model decides which tools to call; Camel copies the caller
 * property into every tool call, where the {@link org.apache.camel.example.aitools.policy.ToolAuthorizationPolicy}
 * uses it to authorize the call in-process. The caller identity is established here, once, from the validated token,
 * so nothing the model does can change who it is acting as.
 */
public class AssistantRoutes extends RouteBuilder {

    @Override
    public void configure() {
        // the caller is a workload, not the model: an authentication failure is answered with HTTP 401
        onException(JwtSvidException.class, IllegalArgumentException.class)
                .handled(true)
                .setBody(method(RejectionReason.class, "of"))
                .log(LoggingLevel.WARN, "Rejected assistant request: ${body}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
                .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
                .setBody(simple("401 Unauthorized: ${body}"))
                .removeHeaders("CamelSpiffe*");

        from("platform-http:/assistant?httpMethodRestrict=POST").routeId("assistant")
                // keep the user's message: validating the token replaces the body with the parsed JWT-SVID
                .setProperty("userMessage", bodyAs(String.class))
                // authenticate the caller with SPIFFE: the token must be a JWT-SVID minted for the assistant (the
                // audience), signed by the trust domain and still valid. The SPIRE agent checks all of that.
                .setHeader(SpiffeConstants.TOKEN).method(BearerToken.class, "extract")
                .removeHeader("Authorization")
                .to("spiffe:assistant?operation=validateJwtSvid&audience={{assistant.audience}}")
                // the authenticated caller becomes an exchange property, which Camel copies into every tool call and
                // which the model cannot change. This is the identity the tool policy authorizes.
                .setProperty("subject", header(SpiffeConstants.SPIFFE_ID))
                .removeHeaders("CamelSpiffe*")
                .log("Assistant request from ${exchangeProperty.subject}: ${exchangeProperty.userMessage}")
                // hand the agent a system prompt and the user's message, then let it call the tools tagged "support"
                // (defined in ToolBindings). Camel copies this exchange - the subject property included - into each
                // tool call the agent makes.
                .setBody(method(AgentRequest.class, "forUser"))
                .to("langchain4j-agent:assistant?tags=support")
                // the caller logs the answer it receives; keep the assistant side at debug to avoid logging it twice
                .log(LoggingLevel.DEBUG, "Assistant answered ${exchangeProperty.subject}: ${body}");
    }
}
