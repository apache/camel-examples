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

import io.spiffe.exception.JwtSvidException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.apache.camel.component.spiffe.SpiffeConstants;
import org.apache.camel.model.RouteConfigurationDefinition;

/**
 * The workload identity policy shared by the services that expose an HTTP API (the backend and the inventory). It is
 * a route configuration: every route that opts in with {@code routeConfigurationId(WorkloadIdentityPolicy.ID)} gets
 * these checks before its own steps run, so the routes contain business logic only.
 * <ul>
 * <li>Authentication: the request must carry a JWT-SVID as bearer token, minted for this service (the audience). The
 * SPIFFE Workload API checks the signature, the expiry and the audience; a failure is answered with HTTP 401.</li>
 * <li>Authorization: the SPIFFE ID of the caller must be on the allow-list of the route (see {@link AllowList}), or
 * the request is answered with HTTP 403.</li>
 * <li>Audit: every decision is recorded in the {@link AuditTrail} of the service.</li>
 * </ul>
 */
public class WorkloadIdentityPolicy extends RouteConfigurationBuilder {

    /** The id with which the HTTP routes opt in to this policy. */
    public static final String ID = "workload-identity";

    private final String service;
    private final AuditTrail auditTrail;

    /**
     * @param service the name of the service, used to look up its audience ({@code <service>.audience}) and its
     *                allow-lists ({@code <service>.allow.<route id>}) in the configuration
     */
    public WorkloadIdentityPolicy(String service) {
        this.service = service;
        this.auditTrail = new AuditTrail(service);
    }

    public AuditTrail getAuditTrail() {
        return auditTrail;
    }

    @Override
    public void configuration() {
        AllowList allowList = new AllowList(getContext(), service);
        RouteConfigurationDefinition policy = routeConfiguration(ID);

        // whatever goes wrong while checking the token (missing, expired, wrong audience, bad signature, ...)
        // means that the caller is not authenticated
        policy.onException(JwtSvidException.class, IllegalArgumentException.class)
                .handled(true)
                .setBody(method(RejectionReason.class, "of"))
                .bean(auditTrail, "record(${routeId}, null, 'rejected', ${body})")
                .log(LoggingLevel.WARN, "Rejected request to ${routeId}: ${body}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
                .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
                .setBody(simple("401 Unauthorized: ${body}"))
                .removeHeaders("CamelSpiffe*");

        // runs before the first step of every route that uses this policy
        policy.interceptFrom()
                // 1. authentication: the token must be a JWT-SVID minted for this service (the audience), signed by
                //    the trust domain and still valid. The SPIRE agent checks all of that: the message body becomes
                //    the validated io.spiffe.svid.jwtsvid.JwtSvid and the SPIFFE ID of the caller is set as header
                .setHeader(SpiffeConstants.TOKEN).method(BearerToken.class, "extract")
                .removeHeader("Authorization")
                .to("spiffe:" + service + "?operation=validateJwtSvid&audience={{" + service + ".audience}}")
                // 2. authorization: the caller must be on the allow-list of the route
                .choice()
                    .when(method(allowList, "isAllowed(${routeId}, ${header.CamelSpiffeSpiffeId})"))
                        .bean(auditTrail, "record(${routeId}, ${header.CamelSpiffeSpiffeId}, 'allowed', null)")
                        .log("Authenticated caller ${header.CamelSpiffeSpiffeId}, allowed to call ${routeId}")
                    .otherwise()
                        .bean(auditTrail, "record(${routeId}, ${header.CamelSpiffeSpiffeId}, 'denied', null)")
                        .log(LoggingLevel.WARN,
                                "Authenticated caller ${header.CamelSpiffeSpiffeId} is not allowed to call ${routeId}")
                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(403))
                        .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
                        .setBody(simple("403 Forbidden: ${header.CamelSpiffeSpiffeId} is not allowed to call ${routeId}"))
                        .removeHeaders("CamelSpiffe*")
                        // the route itself does not run
                        .stop()
                .end();
    }
}
