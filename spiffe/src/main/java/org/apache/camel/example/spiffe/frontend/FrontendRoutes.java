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
package org.apache.camel.example.spiffe.frontend;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.spiffe.SpiffeConstants;

/**
 * The frontend proves who it is to the backend with a JWT-SVID minted by the SPIFFE Workload API for the backend (the
 * audience of the token). There is no shared secret, password or API key anywhere: the SPIRE agent attests the process
 * and issues short-lived tokens for the identity that was registered for it.
 */
public class FrontendRoutes extends RouteBuilder {

    @Override
    public void configure() {
        // the backend may still be starting, or the SPIRE agent may not have attested this workload yet:
        // log the problem and try again on the next timer tick
        onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.WARN, "Could not call the backend: ${exception.message}");

        // every few seconds: read the orders
        from("timer:orders?period={{frontend.period}}").routeId("orders")
                .setHeader(Exchange.HTTP_PATH, constant("/api/orders"))
                .to("direct:callBackend");

        // less often: read the audit trail of the backend
        from("timer:audit?period={{frontend.audit.period}}&delay={{frontend.audit.period}}").routeId("audit")
                .setHeader(Exchange.HTTP_PATH, constant("/api/audit"))
                .to("direct:callBackend");

        // now and then: ask for a token minted for some other service (the CamelSpiffeAudience header overrides the
        // audience of the endpoint) and present that one instead. The backend must reject it (HTTP 401): the token
        // is genuine, but its audience is not the backend
        from("timer:wrongAudience?period={{frontend.wrongAudience.period}}&delay={{frontend.wrongAudience.period}}")
                .routeId("wrong-audience")
                .setHeader(Exchange.HTTP_PATH, constant("/api/orders"))
                .setHeader(SpiffeConstants.AUDIENCE, simple("{{frontend.wrongAudience}}"))
                .log("Asking for a JWT-SVID with the wrong audience, the backend should reject it")
                .to("direct:callBackend");

        // get a JWT-SVID for the backend and present it as a bearer token, on the path set by the caller
        from("direct:callBackend").routeId("call-backend")
                .to("spiffe:frontend?operation=fetchJwtSvid&audience={{backend.audience}}")
                .log("Fetched a JWT-SVID for ${header.CamelSpiffeSpiffeId} (valid until ${header.CamelSpiffeExpiry})")
                .setHeader("Authorization", simple("Bearer ${body}"))
                // the token travels in the header only, and the CamelSpiffe* headers are of no use to the backend
                .setBody(simple("${null}"))
                .removeHeaders("CamelSpiffe*")
                .to("http://{{backend.host}}:{{backend.port}}?httpMethod=GET&throwExceptionOnFailure=false")
                .log("GET ${header.CamelHttpPath} answered HTTP ${header.CamelHttpResponseCode}: ${body}");
    }
}
