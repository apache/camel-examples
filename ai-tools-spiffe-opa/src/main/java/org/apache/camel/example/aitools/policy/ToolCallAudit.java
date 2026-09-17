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
package org.apache.camel.example.aitools.policy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Remembers the last authorization decisions taken by the {@link ToolAuthorizationPolicy}: which caller asked for which
 * tool, and whether the call was allowed, denied (authenticated, but not permitted that tool) or errored (the policy
 * could not be evaluated). An audit trail of who the assistant let its language model act as, and with which tool, is
 * exactly what an autonomous, tool-using system needs to be accountable.
 */
public class ToolCallAudit {

    private static final int CAPACITY = 50;

    private final Deque<Map<String, Object>> decisions = new ArrayDeque<>();

    public synchronized void record(String caller, String tool, String outcome, String detail) {
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("time", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        decision.put("caller", caller == null ? "anonymous" : caller);
        decision.put("tool", tool);
        decision.put("outcome", outcome);
        if (detail != null) {
            decision.put("detail", detail);
        }
        if (decisions.size() == CAPACITY) {
            decisions.removeFirst();
        }
        decisions.addLast(decision);
    }

    public synchronized List<Map<String, Object>> report() {
        return List.copyOf(decisions);
    }
}
