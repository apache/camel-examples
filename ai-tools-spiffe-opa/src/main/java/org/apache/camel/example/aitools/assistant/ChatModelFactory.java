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

import java.time.Duration;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

/**
 * Builds the Ollama chat model that drives the tool-calling loop. The {@code langchain4j-tools} component autowires the
 * single {@link ChatModel} it finds in the registry, so {@link AssistantApplication} only has to bind the one this
 * returns.
 * <p>
 * The endpoint and the model name come from the environment ({@code OLLAMA_BASE_URL} and {@code OLLAMA_MODEL}) so the
 * same build runs on the host and in a container. The model must support tool calling (for example {@code llama3.1},
 * {@code qwen2.5} or {@code mistral}); temperature is 0 so the assistant behaves predictably.
 */
public final class ChatModelFactory {

    private ChatModelFactory() {
    }

    public static ChatModel create() {
        String baseUrl = envOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
        String model = envOrDefault("OLLAMA_MODEL", "qwen2.5");
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(0.0)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
