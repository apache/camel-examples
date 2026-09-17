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
 * Stands in for the customer system. Returns the customer behind an order, including personal data, which is why the
 * {@code lookupCustomer} tool is not open to every caller: the authorization policy only lets the internal support
 * console use it.
 */
public class CustomerService {

    private static final Map<String, String> CUSTOMERS = Map.of(
            "1001", "Dana Scully, dana.scully@example.com, +1-202-555-0143",
            "1002", "Fox Mulder, fox.mulder@example.com, +1-202-555-0199",
            "1003", "Walter Skinner, walter.skinner@example.com, +1-202-555-0121");

    public String lookup(@Header("orderId") String orderId) {
        return CUSTOMERS.getOrDefault(orderId, "no customer was found for order " + orderId);
    }
}
