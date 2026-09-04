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

import org.apache.camel.Header;

/**
 * Extracts the bearer token from the {@code Authorization} header of an HTTP request.
 */
public class BearerToken {

    private static final String SCHEME = "Bearer ";

    public String extract(@Header("Authorization") String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, SCHEME, 0, SCHEME.length())) {
            throw new IllegalArgumentException("no bearer token in the Authorization header");
        }
        String token = authorization.substring(SCHEME.length()).trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("empty bearer token in the Authorization header");
        }
        return token;
    }
}
