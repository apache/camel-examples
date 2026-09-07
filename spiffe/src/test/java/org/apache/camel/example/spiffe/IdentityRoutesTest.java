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

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import io.spiffe.exception.X509ContextException;
import io.spiffe.spiffeid.SpiffeId;
import io.spiffe.svid.x509svid.X509Svid;
import io.spiffe.workloadapi.WorkloadApiClient;
import io.spiffe.workloadapi.X509Context;
import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.component.spiffe.SpiffeConstants;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.test.main.junit6.CamelMainTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the identity route against a fake SPIFFE Workload API that hands out a self-signed certificate with the SPIFFE
 * ID of the frontend as URI subject alternative name (see src/test/resources/frontend-svid.pem).
 */
class IdentityRoutesTest extends CamelMainTestSupport {

    @BindToRegistry
    private final WorkloadApiClient workloadApiClient = mock(WorkloadApiClient.class);

    @Override
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addRoutesBuilder(IdentityRoutes.class);
    }

    @Override
    public void setupResources() throws Exception {
        // trigger the route by hand instead of waiting for the timer
        camelContextConfiguration().replaceRouteFromWith("identity", "direct:identity");
        super.setupResources();
    }

    @Test
    void describesTheX509SvidWithoutRevealingTheKey() throws Exception {
        X509Certificate leaf = loadCertificate("/frontend-svid.pem");
        X509Svid svid = mock(X509Svid.class);
        when(svid.getSpiffeId()).thenReturn(SpiffeId.parse("spiffe://example.org/frontend"));
        when(svid.getLeaf()).thenReturn(leaf);
        when(svid.getChain()).thenReturn(List.of(leaf));
        X509Context x509Context = mock(X509Context.class);
        when(x509Context.getDefaultSvid()).thenReturn(svid);
        when(workloadApiClient.fetchX509Context()).thenReturn(x509Context);

        Exchange out = template.request("direct:identity", exchange -> {
        });

        String summary = out.getMessage().getBody(String.class);
        assertTrue(summary.startsWith("X.509-SVID of spiffe://example.org/frontend"), summary);
        assertTrue(summary.contains("subject       : CN=frontend, O=Apache Camel, C=US"), summary);
        assertTrue(summary.contains("URI SANs      : [spiffe://example.org/frontend]"), summary);
        assertTrue(summary.contains("chain length  : 1 certificate(s)"), summary);
        assertEquals("spiffe://example.org/frontend", out.getMessage().getHeader(SpiffeConstants.SPIFFE_ID));
        verify(svid, never()).getPrivateKey();
    }

    @Test
    void failuresAreLoggedAndDoNotStopTheRoute() throws Exception {
        when(workloadApiClient.fetchX509Context()).thenThrow(new X509ContextException("no identity issued"));

        Exchange out = template.request("direct:identity", exchange -> {
        });

        assertFalse(out.isFailed(), "the failure is handled by the route");
    }

    private static X509Certificate loadCertificate(String resource) throws Exception {
        try (InputStream in = IdentityRoutesTest.class.getResourceAsStream(resource)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }
}
