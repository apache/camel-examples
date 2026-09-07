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

import org.apache.camel.Exchange;
import org.apache.camel.ExchangeProperty;

/**
 * Explains why a request was rejected, from the exception that stopped it. When the SPIFFE Workload API refuses a
 * token, the java-spiffe library reports a generic "Error validating JWT SVID" and keeps the actual reason (expired,
 * wrong audience, unknown key, ...) in the cause, so that one is added to the explanation.
 */
public class RejectionReason {

    public String of(@ExchangeProperty(Exchange.EXCEPTION_CAUGHT) Exception exception) {
        StringBuilder reason = new StringBuilder(exception.getMessage());
        Throwable cause = exception.getCause();
        if (cause != null && cause.getMessage() != null) {
            reason.append(": ").append(cause.getMessage());
        }
        return reason.toString();
    }
}
