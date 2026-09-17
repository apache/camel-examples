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

import org.apache.camel.component.langchain4j.agent.api.AgentConfiguration;
import org.apache.camel.example.aitools.IdentityRoutes;
import org.apache.camel.example.aitools.policy.ToolAuthorizationPolicy;
import org.apache.camel.example.aitools.policy.ToolCallAudit;
import org.apache.camel.main.Main;

/**
 * Boots the AI support assistant: an HTTP API that authenticates its callers with SPIFFE, runs an Ollama chat model
 * with a set of tools, and authorizes every tool call in-process against a WebAssembly policy.
 */
public final class AssistantApplication {

    private AssistantApplication() {
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        // the embedded HTTP server the callers post to
        main.configure().httpServer().withEnabled(true).withPort(8080);
        // the chat model that drives the tool-calling loop; the langchain4j-agent autowires this configuration
        main.bind("agentConfiguration", new AgentConfiguration().withChatModel(ChatModelFactory.create()));
        // the tools, the work behind them, and the authorization policy that guards each tool call
        ToolCallAudit audit = new ToolCallAudit();
        main.bind("toolCallAudit", audit);
        main.configure().addRoutesBuilder(new ToolAuthorizationPolicy(audit));
        main.configure().addRoutesBuilder(new ToolRoutes(new RefundLedger()));
        main.configure().addRoutesBuilder(new ToolBindings());
        main.configure().addRoutesBuilder(new AssistantRoutes());
        main.configure().addRoutesBuilder(IdentityRoutes.class);
        // now keep the application running until the JVM is terminated (ctrl + c or sigterm)
        main.run(args);
    }
}
