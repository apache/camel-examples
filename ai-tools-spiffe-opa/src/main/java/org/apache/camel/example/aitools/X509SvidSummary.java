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
package org.apache.camel.example.aitools;

import java.security.cert.X509Certificate;

import io.spiffe.svid.x509svid.X509Svid;

/**
 * Turns an {@link X509Svid} into a one-line summary of its leaf certificate: the SPIFFE ID, the serial number and the
 * expiry. The serial and the expiry change each time the SPIRE agent rotates the certificate, so watching this line is
 * enough to see the rotation. Only the certificate is described, never the private key that comes with the SVID.
 */
public class X509SvidSummary {

    public String describe(X509Svid svid) {
        X509Certificate leaf = svid.getLeaf();
        return String.format("X.509-SVID of %s (serial %s, valid until %s)",
                svid.getSpiffeId(),
                leaf.getSerialNumber().toString(16),
                leaf.getNotAfter().toInstant());
    }
}
