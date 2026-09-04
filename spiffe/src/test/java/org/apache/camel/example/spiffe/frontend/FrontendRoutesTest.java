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

import java.util.Date;

import io.spiffe.exception.JwtSvidException;
import io.spiffe.spiffeid.SpiffeId;
import io.spiffe.svid.jwtsvid.JwtSvid;
import io.spiffe.workloadapi.WorkloadApiClient;
import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.spiffe.SpiffeConstants;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.test.main.junit6.CamelMainTestSupport;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the frontend routes against a fake SPIFFE Workload API (a mocked {@link WorkloadApiClient} that the spiffe
 * component autowires from the registry) and a mock endpoint in place of the backend.
 */
class FrontendRoutesTest extends CamelMainTestSupport {

    private static final String BACKEND = "spiffe://example.org/backend";

    @BindToRegistry
    private final WorkloadApiClient workloadApiClient = mock(WorkloadApiClient.class);

    @Override
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addRoutesBuilder(FrontendRoutes.class);
    }

    @Override
    public void setupResources() throws Exception {
        // trigger the routes by hand instead of waiting for the timers
        camelContextConfiguration().replaceRouteFromWith("orders", "direct:orders");
        camelContextConfiguration().replaceRouteFromWith("audit", "direct:audit");
        camelContextConfiguration().replaceRouteFromWith("wrong-audience", "direct:wrongAudience");
        super.setupResources();
    }

    @Override
    public boolean isUseAdviceWith() {
        // the routes are advised before the context is started, to swap the real backend for a mock endpoint
        return true;
    }

    @Test
    void readsTheOrdersWithAJwtSvidAsBearerToken() throws Exception {
        MockEndpoint backend = mockTheBackend();
        JwtSvid svid = jwtSvid("frontend-token");
        when(workloadApiClient.fetchJwtSvid(BACKEND)).thenReturn(svid);

        backend.expectedMessageCount(1);
        backend.expectedHeaderReceived(Exchange.HTTP_PATH, "/api/orders");
        backend.expectedHeaderReceived("Authorization", "Bearer frontend-token");
        backend.message(0).body().isNull();
        backend.message(0).header(SpiffeConstants.SPIFFE_ID).isNull();
        backend.message(0).header(SpiffeConstants.EXPIRY).isNull();

        template.sendBody("direct:orders", null);

        backend.assertIsSatisfied();
    }

    @Test
    void readsTheAuditTrail() throws Exception {
        MockEndpoint backend = mockTheBackend();
        JwtSvid svid = jwtSvid("frontend-token");
        when(workloadApiClient.fetchJwtSvid(BACKEND)).thenReturn(svid);

        backend.expectedMessageCount(1);
        backend.expectedHeaderReceived(Exchange.HTTP_PATH, "/api/audit");
        backend.expectedHeaderReceived("Authorization", "Bearer frontend-token");

        template.sendBody("direct:audit", null);

        backend.assertIsSatisfied();
    }

    @Test
    void asksForATokenWithTheWrongAudienceOnPurpose() throws Exception {
        MockEndpoint backend = mockTheBackend();
        JwtSvid svid = jwtSvid("token-for-another-service");
        when(workloadApiClient.fetchJwtSvid("spiffe://example.org/some-other-service")).thenReturn(svid);

        backend.expectedMessageCount(1);
        backend.expectedHeaderReceived("Authorization", "Bearer token-for-another-service");
        backend.message(0).header(SpiffeConstants.AUDIENCE).isNull();

        template.sendBody("direct:wrongAudience", null);

        backend.assertIsSatisfied();
    }

    @Test
    void failuresAreLoggedAndDoNotStopTheRoute() throws Exception {
        MockEndpoint backend = mockTheBackend();
        when(workloadApiClient.fetchJwtSvid(BACKEND)).thenThrow(new JwtSvidException("no identity issued"));

        backend.expectedMessageCount(0);

        // the failure is handled by the route, so it does not propagate to the caller
        template.sendBody("direct:orders", null);

        backend.assertIsSatisfied();
    }

    private MockEndpoint mockTheBackend() throws Exception {
        AdviceWith.adviceWith(context, "call-backend",
                advice -> advice.weaveByToUri("http:*").replace().to("mock:backend"));
        context.start();
        return getMockEndpoint("mock:backend");
    }

    private static JwtSvid jwtSvid(String token) {
        JwtSvid svid = mock(JwtSvid.class);
        when(svid.getToken()).thenReturn(token);
        when(svid.getSpiffeId()).thenReturn(SpiffeId.parse("spiffe://example.org/frontend"));
        when(svid.getExpiry()).thenReturn(new Date(System.currentTimeMillis() + 300_000));
        return svid;
    }
}
