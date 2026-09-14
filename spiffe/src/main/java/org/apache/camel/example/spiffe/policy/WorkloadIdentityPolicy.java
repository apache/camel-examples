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
import org.apache.camel.component.opa.OpaConstants;
import org.apache.camel.component.opa.OpaPolicyEvaluationException;
import org.apache.camel.component.spiffe.SpiffeConstants;
import org.apache.camel.model.RouteConfigurationDefinition;

/**
 * The workload identity policy shared by the services that expose an HTTP API (the backend and the inventory). It is
 * a route configuration: every route that opts in with {@code routeConfigurationId(WorkloadIdentityPolicy.ID)} gets
 * these checks before its own steps run, so the routes contain business logic only.
 * <ul>
 * <li>Authentication, with SPIFFE: the request must carry a JWT-SVID as bearer token, minted for this service (the
 * audience). The SPIFFE Workload API checks the signature, the expiry and the audience; a failure is answered with
 * HTTP 401.</li>
 * <li>Authorization, with Open Policy Agent: the SPIFFE ID of the caller and the id of the route are sent to OPA,
 * which evaluates the Rego policy of this service (see the opa directory of the example). A deny is answered with
 * HTTP 403, and a policy that cannot be evaluated with HTTP 503: the policy fails closed.</li>
 * <li>Audit: every decision is recorded in the {@link AuditTrail} of the service.</li>
 * </ul>
 */
public class WorkloadIdentityPolicy extends RouteConfigurationBuilder {

    /** The id with which the HTTP routes opt in to this policy. */
    public static final String ID = "workload-identity";

    private final String service;
    private final AuditTrail auditTrail;

    /**
     * @param service the name of the service, used to look up its audience ({@code <service>.audience}) in the
     *                configuration and its policy ({@code camel/spiffe/<service>/allow}) in OPA
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

        // OPA could not decide (unreachable, an error, an undefined decision): nobody gets in
        policy.onException(OpaPolicyEvaluationException.class)
                .handled(true)
                .bean(auditTrail, "record(${routeId}, ${header.CamelSpiffeSpiffeId}, 'error', ${exception.message})")
                .log(LoggingLevel.ERROR, "Could not evaluate the policy for ${routeId}: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(503))
                .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
                .setBody(constant("503 Service Unavailable: the policy could not be evaluated"))
                .removeHeaders("CamelSpiffe*");

        // runs before the first step of every route that uses this policy
        policy.interceptFrom()
                // 1. authentication: the token must be a JWT-SVID minted for this service (the audience), signed by
                //    the trust domain and still valid. The SPIRE agent checks all of that: the message body becomes
                //    the validated io.spiffe.svid.jwtsvid.JwtSvid and the SPIFFE ID of the caller is set as header
                .setHeader(SpiffeConstants.TOKEN).method(BearerToken.class, "extract")
                .removeHeader("Authorization")
                .to("spiffe:" + service + "?operation=validateJwtSvid&audience={{" + service + ".audience}}")
                // 2. authorization: OPA gets the SPIFFE ID of the caller (and, on the second hop, on whose behalf it
                //    calls) together with the id of the route, and evaluates the policy of this service. Only those
                //    two headers are sent: the token stays here
                .to("opa:camel/spiffe/" + service + "/allow?serverUrl={{opa.url}}"
                    + "&includeHeaders=" + SpiffeConstants.SPIFFE_ID + ",X-On-Behalf-Of")
                .choice()
                    .when(header(OpaConstants.DECISION_ALLOW).isEqualTo(true))
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
                .end()
                // the decision headers are of no use to the route
                .removeHeaders("CamelOpa*");
    }
}
