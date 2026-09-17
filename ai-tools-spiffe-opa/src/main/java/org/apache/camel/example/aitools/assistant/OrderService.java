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
package org.apache.camel.example.aitools.assistant;

import java.util.Map;

import org.apache.camel.Header;

/**
 * Stands in for the real order system. Returns the status of an order as a short line of text, which is what the
 * language model reads back as the result of the {@code getOrderStatus} tool.
 */
public class OrderService {

    private static final Map<String, String> STATUS = Map.of(
            "1001", "order 1001 (Camel in Action, 2nd edition) shipped, arriving tomorrow",
            "1002", "order 1002 (Enterprise Integration Patterns) delivered on 2026-09-10",
            "1003", "order 1003 (Zero Trust Networks) is being prepared for shipping");

    public String status(@Header("orderId") String orderId) {
        return STATUS.getOrDefault(orderId, "no order was found with id " + orderId);
    }
}
