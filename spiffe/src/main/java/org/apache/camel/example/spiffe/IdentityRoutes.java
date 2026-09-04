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
package org.apache.camel.example.spiffe;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

/**
 * Fetches the X.509-SVID of this workload from the SPIFFE Workload API at regular intervals and logs a summary of it.
 * <p>
 * Both applications of this example run this very same route, yet each of them is issued a different identity: the
 * SPIRE agent attests the process that connects to the Workload API (in this example by its Unix user id) and looks up
 * the registration entry that matches it. Identity comes from the platform, not from the code or its configuration.
 * <p>
 * The X.509-SVID is short-lived and the SPIRE agent rotates it before it expires, so the serial number and the validity
 * period in the log change over time without the application doing anything about it.
 */
public class IdentityRoutes extends RouteBuilder {

    @Override
    public void configure() {
        // the SPIRE agent may not have attested this workload yet: log the problem and try again on the next tick
        onException(Exception.class)
                .handled(true)
                .log(LoggingLevel.WARN, "Could not fetch the X.509-SVID: ${exception.message}");

        from("timer:identity?period={{identity.period}}").routeId("identity")
                // fetchX509Svid is also the default operation of the component. The message body becomes an
                // io.spiffe.svid.x509svid.X509Svid and the SPIFFE ID is set as the CamelSpiffeSpiffeId header
                .to("spiffe:identity?operation=fetchX509Svid")
                // the X509Svid also carries the private key of the workload, so never log the body as-is
                .bean(X509SvidSummary.class, "describe")
                .log("${body}");
    }
}
