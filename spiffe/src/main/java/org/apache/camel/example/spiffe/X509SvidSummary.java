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

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.spiffe.svid.x509svid.X509Svid;

/**
 * Turns an {@link X509Svid} into a human-readable summary of its leaf certificate. Only the certificate is described:
 * the private key that comes with the SVID is never printed.
 */
public class X509SvidSummary {

    /**
     * The GeneralName type of a uniformResourceIdentifier subject alternative name, which is where a SPIFFE ID is
     * encoded in an X.509-SVID.
     */
    private static final int URI_NAME = 6;

    public String describe(X509Svid svid) throws CertificateParsingException {
        X509Certificate leaf = svid.getLeaf();
        return String.format("""
                X.509-SVID of %s
                    serial number : %s
                    subject       : %s
                    issuer        : %s
                    valid from    : %s
                    valid until   : %s
                    URI SANs      : %s
                    chain length  : %d certificate(s)""",
                svid.getSpiffeId(),
                leaf.getSerialNumber().toString(16),
                leaf.getSubjectX500Principal(),
                leaf.getIssuerX500Principal(),
                leaf.getNotBefore().toInstant(),
                leaf.getNotAfter().toInstant(),
                uriSubjectAlternativeNames(leaf),
                svid.getChain().size());
    }

    private static List<String> uriSubjectAlternativeNames(X509Certificate certificate)
            throws CertificateParsingException {
        List<String> uris = new ArrayList<>();
        Collection<List<?>> names = certificate.getSubjectAlternativeNames();
        if (names != null) {
            for (List<?> name : names) {
                if (Integer.valueOf(URI_NAME).equals(name.get(0))) {
                    uris.add(String.valueOf(name.get(1)));
                }
            }
        }
        return uris;
    }
}
