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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Remembers the last decisions taken by the {@link WorkloadIdentityPolicy} of a service: who called which route, and
 * whether the request was allowed, denied (authenticated, but not on the allow-list) or rejected (not authenticated).
 */
public class AuditTrail {

    private static final int CAPACITY = 20;

    private final String service;
    private final Deque<Map<String, Object>> decisions = new ArrayDeque<>();

    public AuditTrail(String service) {
        this.service = service;
    }

    public synchronized void record(String route, String caller, String outcome, String detail) {
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("time", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        decision.put("route", route);
        decision.put("caller", caller == null ? "anonymous" : caller);
        decision.put("outcome", outcome);
        if (detail != null) {
            decision.put("detail", detail);
        }
        if (decisions.size() == CAPACITY) {
            decisions.removeFirst();
        }
        decisions.addLast(decision);
    }

    public synchronized Map<String, Object> report() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("service", service);
        report.put("decisions", List.copyOf(decisions));
        return report;
    }
}
