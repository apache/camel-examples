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
package org.apache.camel.example.spiffe.inventory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.Map;

import com.styra.opa.OPAClient;
import io.spiffe.spiffeid.SpiffeId;
import io.spiffe.svid.jwtsvid.JwtSvid;
import io.spiffe.workloadapi.WorkloadApiClient;
import org.apache.camel.BindToRegistry;
import org.apache.camel.example.spiffe.policy.WorkloadIdentityPolicy;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.main.junit6.CamelMainTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the inventory over HTTP, on the embedded server of Camel Main, against a fake SPIFFE Workload API and a fake
 * OPA that decides like opa/inventory.rego does.
 */
class InventoryRoutesTest extends CamelMainTestSupport {

    private static final String INVENTORY = "spiffe://example.org/inventory";
    private static final String POLICY = "camel/spiffe/inventory/allow";

    // static, because configureContext() runs in the constructor of CamelTestSupport, before the instance
    // fields are initialized
    private static final int PORT = AvailablePortFinder.getNextAvailable();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @BindToRegistry
    private final WorkloadApiClient workloadApiClient = mock(WorkloadApiClient.class);

    @BindToRegistry
    private final OPAClient opaClient = mock(OPAClient.class);

    @Override
    protected void configure(MainConfigurationProperties configuration) {
        configuration.httpServer().withEnabled(true).withPort(PORT);
        configuration.addRoutesBuilder(new WorkloadIdentityPolicy("inventory"));
        configuration.addRoutesBuilder(InventoryRoutes.class);
    }

    @Test
    void backendGetsTheStockLevels() throws Exception {
        opaDecidesLikeThePolicy();
        JwtSvid backend = jwtSvid("spiffe://example.org/backend");
        when(workloadApiClient.validateJwtSvid("backend-token", INVENTORY)).thenReturn(backend);

        HttpResponse<String> response = get("Bearer backend-token", "spiffe://example.org/frontend");

        assertEquals(200, response.statusCode());
        assertEquals("{\"Camel in Action, 2nd edition\":12,\"Enterprise Integration Patterns\":0,\"Zero Trust Networks\":5}",
                response.body());
    }

    @Test
    void frontendMayNotAskTheInventoryDirectly() throws Exception {
        opaDecidesLikeThePolicy();
        JwtSvid frontend = jwtSvid("spiffe://example.org/frontend");
        when(workloadApiClient.validateJwtSvid("frontend-token", INVENTORY)).thenReturn(frontend);

        HttpResponse<String> response = get("Bearer frontend-token", null);

        assertEquals(403, response.statusCode());
        assertEquals("403 Forbidden: spiffe://example.org/frontend is not allowed to call stock",
                response.body());
    }

    @Test
    void backendMayNotAskOnBehalfOfSomeoneWhoMayNotReadTheOrders() throws Exception {
        opaDecidesLikeThePolicy();
        JwtSvid backend = jwtSvid("spiffe://example.org/backend");
        when(workloadApiClient.validateJwtSvid("backend-token", INVENTORY)).thenReturn(backend);

        HttpResponse<String> response = get("Bearer backend-token", "spiffe://example.org/auditor");

        assertEquals(403, response.statusCode());
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        opaDecidesLikeThePolicy();
        HttpResponse<String> response = get(null, null);

        assertEquals(401, response.statusCode());
    }

    /**
     * The fake OPA decides like opa/inventory.rego does: the backend, on behalf of a caller that may read the orders.
     */
    private void opaDecidesLikeThePolicy() throws Exception {
        when(opaClient.evaluate(eq(POLICY), anyMap(), eq(Object.class))).thenAnswer(invocation -> {
            Map<String, Object> input = invocation.getArgument(1);
            Map<?, ?> headers = (Map<?, ?>) input.get("headers");
            Object caller = headers.get("CamelSpiffeSpiffeId");
            Object onBehalfOf = headers.entrySet().stream()
                    .filter(header -> "X-On-Behalf-Of".equalsIgnoreCase(String.valueOf(header.getKey())))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            return "spiffe://example.org/backend".equals(caller) && "spiffe://example.org/frontend".equals(onBehalfOf);
        });
    }

    private static HttpResponse<String> get(String authorization, String onBehalfOf) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + PORT + "/api/stock")).GET();
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        if (onBehalfOf != null) {
            request.header("X-On-Behalf-Of", onBehalfOf);
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JwtSvid jwtSvid(String spiffeId) {
        JwtSvid svid = mock(JwtSvid.class);
        when(svid.getSpiffeId()).thenReturn(SpiffeId.parse(spiffeId));
        when(svid.getExpiry()).thenReturn(new Date(System.currentTimeMillis() + 300_000));
        return svid;
    }
}
