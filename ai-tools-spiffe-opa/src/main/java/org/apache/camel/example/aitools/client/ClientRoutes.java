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
package org.apache.camel.example.aitools.client;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

/**
 * A caller of the assistant. It proves who it is with a JWT-SVID minted by the SPIFFE Workload API for the assistant
 * (the audience of the token); there is no shared secret, password or API key. The SPIRE agent attests the process and
 * issues short-lived tokens for the identity that was registered for it.
 * <p>
 * The public chatbot and the support console both run this same route as different Unix users, so each is issued a
 * different SPIFFE ID, and the assistant's policy grants each of them different tools. What each one asks is set with
 * {@code client.message} (the {@code CLIENT_MESSAGE} environment variable in compose.yaml).
 */
public class ClientRoutes extends RouteBuilder {

    @Override
    public void configure() {
        // the assistant may still be starting, or the SPIRE agent may not have attested this workload yet:
        // log the problem and try again on the next timer tick
        onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.WARN, "Could not call the assistant: ${exception.message}");

        from("timer:ask?period={{client.period}}&delay={{client.delay}}").routeId("ask-assistant")
                // get a JWT-SVID minted for the assistant and present it as a bearer token
                .to("spiffe:client?operation=fetchJwtSvid&audience={{assistant.audience}}")
                .log(LoggingLevel.DEBUG,
                        "Fetched a JWT-SVID for ${header.CamelSpiffeSpiffeId} (valid until ${header.CamelSpiffeExpiry})")
                .setHeader("Authorization", simple("Bearer ${body}"))
                // the natural-language request goes in the body
                .setBody(simple("{{client.message}}"))
                .removeHeaders("CamelSpiffe*")
                .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to("http://{{assistant.host}}:{{assistant.port}}/assistant?throwExceptionOnFailure=false")
                .log("Assistant replied (HTTP ${header.CamelHttpResponseCode}): ${body}");
    }
}
