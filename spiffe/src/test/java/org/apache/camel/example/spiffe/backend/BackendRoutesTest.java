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
package org.apache.camel.example.spiffe.backend;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import io.spiffe.exception.JwtSvidException;
import io.spiffe.spiffeid.SpiffeId;
import io.spiffe.svid.jwtsvid.JwtSvid;
import io.spiffe.workloadapi.WorkloadApiClient;
import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.spiffe.SpiffeConstants;
import org.apache.camel.example.spiffe.policy.WorkloadIdentityPolicy;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.junit6.CamelContextConfiguration;
import org.apache.camel.test.main.junit6.CamelMainTestSupport;
import org.junit.jupiter.api.Test;

import static org.apache.camel.util.PropertiesHelper.asProperties;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the backend over HTTP, on the embedded server of Camel Main, against a fake SPIFFE Workload API and a stub of
 * the inventory service. The spiffe component autowires the single {@link WorkloadApiClient} it finds in the
 * registry, so the routes and the policy under test are exactly the ones used at runtime: only the SPIRE agent is
 * replaced.
 */
class BackendRoutesTest extends CamelMainTestSupport {

    private static final String BACKEND = "spiffe://example.org/backend";
    private static final String INVENTORY = "spiffe://example.org/inventory";
    private static final String FRONTEND = "spiffe://example.org/frontend";
    private static final String AUDITOR = "spiffe://example.org/auditor";
    private static final String STOCK_LEVELS
            = "{\"Camel in Action, 2nd edition\":12,\"Enterprise Integration Patterns\":0,\"Zero Trust Networks\":5}";

    // static, because configureContext() runs in the constructor of CamelTestSupport, before the instance
    // fields are initialized
    private static final int PORT = AvailablePortFinder.getNextAvailable();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private final WorkloadIdentityPolicy policy = new WorkloadIdentityPolicy("backend");
    /** The headers of the last request received by the stub inventory. */
    private final Map<String, Object> inventoryRequestHeaders = new ConcurrentHashMap<>();

    @BindToRegistry
    private final WorkloadApiClient workloadApiClient = mock(WorkloadApiClient.class);

    @Override
    protected void configure(MainConfigurationProperties configuration) {
        configuration.httpServer().withEnabled(true).withPort(PORT);
        configuration.addRoutesBuilder(policy);
        configuration.addRoutesBuilder(new BackendRoutes(policy.getAuditTrail()));
        // a stub of the inventory service, on the same embedded server
        configuration.addRoutesBuilder(new RouteBuilder() {
            @Override
            public void configure() {
                from("platform-http:/api/stock").routeId("stub-inventory")
                        .process(exchange -> {
                            inventoryRequestHeaders.clear();
                            exchange.getMessage().getHeaders().forEach((name, value) -> {
                                if (value != null) {
                                    inventoryRequestHeaders.put(name, value);
                                }
                            });
                        })
                        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                        .setBody(constant(STOCK_LEVELS));
            }
        });
    }

    @Override
    public void configureContext(CamelContextConfiguration camelContextConfiguration) {
        super.configureContext(camelContextConfiguration);
        Properties overrides = asProperties("inventory.host", "localhost", "inventory.port", Integer.toString(PORT));
        camelContextConfiguration.withUseOverridePropertiesWithPropertiesComponent(overrides);
    }

    @Test
    void frontendGetsTheOrdersWithTheStockLevels() throws Exception {
        JwtSvid frontend = jwtSvid(FRONTEND, null);
        when(workloadApiClient.validateJwtSvid("frontend-token", BACKEND)).thenReturn(frontend);
        JwtSvid backend = jwtSvid(BACKEND, "backend-token");
        when(workloadApiClient.fetchJwtSvid(INVENTORY)).thenReturn(backend);

        HttpResponse<String> response = get("/api/orders", "Bearer frontend-token");

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.contains("\"caller\":\"spiffe://example.org/frontend\""), body);
        assertTrue(body.contains("\"item\":\"Camel in Action, 2nd edition\",\"quantity\":2,\"inStock\":true"), body);
        assertTrue(body.contains("\"item\":\"Enterprise Integration Patterns\",\"quantity\":1,\"inStock\":false"), body);

        // the second hop was made with the identity of the backend, on behalf of the frontend
        assertEquals("Bearer backend-token", inventoryRequestHeaders.get("Authorization"));
        assertEquals(FRONTEND, inventoryRequestHeaders.get("X-On-Behalf-Of"));

        // and nothing of it leaks back to the caller
        for (String header : new String[] {
                "Authorization", "X-On-Behalf-Of", SpiffeConstants.TOKEN, SpiffeConstants.SPIFFE_ID }) {
            assertTrue(response.headers().firstValue(header).isEmpty(), header + " must not be in the response");
        }
    }

    @Test
    void auditorMayNotReadTheOrders() throws Exception {
        JwtSvid auditor = jwtSvid(AUDITOR, null);
        when(workloadApiClient.validateJwtSvid("auditor-token", BACKEND)).thenReturn(auditor);

        HttpResponse<String> response = get("/api/orders", "Bearer auditor-token");

        assertEquals(403, response.statusCode());
        assertEquals("403 Forbidden: spiffe://example.org/auditor is not allowed to call orders",
                response.body());
    }

    @Test
    void auditorReadsTheAuditTrail() throws Exception {
        JwtSvid auditor = jwtSvid(AUDITOR, null);
        when(workloadApiClient.validateJwtSvid("auditor-token", BACKEND)).thenReturn(auditor);

        // a denied call first, so that there is something to audit
        get("/api/orders", "Bearer auditor-token");
        HttpResponse<String> response = get("/api/audit", "Bearer auditor-token");

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertTrue(body.startsWith("{\"service\":\"backend\",\"decisions\":["), body);
        assertTrue(body.contains("\"route\":\"orders\",\"caller\":\"spiffe://example.org/auditor\",\"outcome\":\"denied\""),
                body);
        assertTrue(body.contains("\"route\":\"audit\",\"caller\":\"spiffe://example.org/auditor\",\"outcome\":\"allowed\""),
                body);
    }

    @Test
    void frontendMayNotReadTheAuditTrail() throws Exception {
        JwtSvid frontend = jwtSvid(FRONTEND, null);
        when(workloadApiClient.validateJwtSvid("frontend-token", BACKEND)).thenReturn(frontend);

        HttpResponse<String> response = get("/api/audit", "Bearer frontend-token");

        assertEquals(403, response.statusCode());
    }

    @Test
    void invalidTokenIsUnauthorized() throws Exception {
        // this is how the java-spiffe library reports a token that the Workload API refused
        when(workloadApiClient.validateJwtSvid("token-for-another-service", BACKEND))
                .thenThrow(new JwtSvidException("Error validating JWT SVID",
                        new IllegalStateException("expected audience in [spiffe://example.org/backend]")));

        HttpResponse<String> response = get("/api/orders", "Bearer token-for-another-service");

        assertEquals(401, response.statusCode());
        assertEquals("401 Unauthorized: Error validating JWT SVID: expected audience in [spiffe://example.org/backend]",
                response.body());
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        HttpResponse<String> response = get("/api/orders", null);

        assertEquals(401, response.statusCode());
        assertEquals("401 Unauthorized: no bearer token in the Authorization header",
                response.body());
    }

    @Test
    void unreachableInventoryIsABadGateway() throws Exception {
        JwtSvid frontend = jwtSvid(FRONTEND, null);
        when(workloadApiClient.validateJwtSvid("frontend-token", BACKEND)).thenReturn(frontend);
        when(workloadApiClient.fetchJwtSvid(INVENTORY)).thenThrow(new JwtSvidException("no identity issued"));

        HttpResponse<String> response = get("/api/orders", "Bearer frontend-token");

        assertEquals(502, response.statusCode());
        assertEquals("502 Bad Gateway: no identity issued", response.body());
    }

    private static HttpResponse<String> get(String path, String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + PORT + path)).GET();
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JwtSvid jwtSvid(String spiffeId, String token) {
        JwtSvid svid = mock(JwtSvid.class);
        when(svid.getSpiffeId()).thenReturn(SpiffeId.parse(spiffeId));
        when(svid.getToken()).thenReturn(token);
        when(svid.getExpiry()).thenReturn(new Date(System.currentTimeMillis() + 300_000));
        return svid;
    }
}
