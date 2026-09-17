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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.camel.Header;

/**
 * Stands in for the payments system, recording the refunds that the {@code refundOrder} tool is allowed to make. This
 * is the money-moving tool, so it is the one the authorization policy guards most tightly: only the internal support
 * console may call it, and only up to the amount the policy allows. By the time {@link #refund} runs, the policy has
 * already had its say; a refund recorded here is, by construction, one that was authorized.
 */
public class RefundLedger {

    private final List<String> refunds = new CopyOnWriteArrayList<>();

    public String refund(@Header("orderId") String orderId, @Header("amount") String amount) {
        refunds.add(orderId + ":" + amount);
        return "refunded " + amount + " dollars on order " + orderId;
    }

    /** The refunds recorded so far, each as {@code orderId:amount}. */
    public List<String> refunds() {
        return List.copyOf(refunds);
    }
}
