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

import org.apache.camel.ExchangeProperty;
import org.apache.camel.component.langchain4j.agent.api.AiAgentBody;

/**
 * Builds the request sent to the {@code langchain4j-agent}: the caller's message as the user message, plus a system
 * message that sets the assistant's role. The system prompt tells the model to rely on the tools and to respect a
 * refusal, but it is not a security control: the guarantee comes from the
 * {@link org.apache.camel.example.aitools.policy.ToolAuthorizationPolicy}, which stops a tool call the caller is not
 * allowed to make whatever the model was talked into doing.
 */
public class AgentRequest {

    private static final String SYSTEM_PROMPT = """
            You are a customer-support assistant for an online bookshop.
            Answer the user's request using the tools available to you:
              - getOrderStatus to report the status of an order,
              - lookupCustomer to retrieve the customer behind an order,
              - refundOrder to refund an order by a given amount.
            Call a tool rather than guessing. If a tool responds that access is denied, tell the user plainly that you
            are not allowed to do that, and do not attempt to work around it. Keep your answers short.""";

    public AiAgentBody<?> forUser(@ExchangeProperty("userMessage") String message) {
        return new AiAgentBody<>().withUserMessage(message).withSystemMessage(SYSTEM_PROMPT);
    }
}
