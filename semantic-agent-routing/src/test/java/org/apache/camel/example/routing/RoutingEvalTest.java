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
package org.apache.camel.example.routing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.main.Main;
import org.apache.camel.semantic.SemanticResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Opt-in model evaluation: one batched decision call per labelled request, no text generation. */
@EnabledIfSystemProperty(named = "routing.eval", matches = "true")
class RoutingEvalTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void evaluateSelectedProvider() throws Exception {
        JsonNode cases;
        try (var input = getClass().getResourceAsStream("/routing-eval.json")) {
            cases = JSON.readTree(input);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        int selectedMatches = 0;
        int choiceMatches = 0;
        Main main = new Main(RoutingApplication.class);
        main.addOverrideProperty("role", "coordinator");
        main.addOverrideProperty("camel.server.enabled", "false");
        main.configure().withRoutesIncludePattern("classpath:routes/coordinator.yaml")
                .withRouteFilterIncludePattern("select-specialists");
        try {
            main.start();
            try (var producer = main.getCamelContext().createProducerTemplate()) {
                for (JsonNode item : cases) {
                    long start = System.nanoTime();
                    var exchange = producer.request("direct:select-specialists",
                            e -> e.getMessage().setBody(item.path("request").asText()));
                    if (exchange.getException() != null) {
                        throw exchange.getException();
                    }
                    @SuppressWarnings("unchecked")
                    List<String> selected = exchange.getProperty("specialists", List.class);
                    @SuppressWarnings("unchecked")
                    Map<String, SemanticResult> results = exchange.getProperty("CamelSemanticResults", Map.class);
                    String choice = results.get("specialist").getValue().toString();
                    boolean selectedMatch = matches(selected, item.path("expected"));
                    boolean choiceMatch = matches(List.of(choice), item.path("expected"));
                    selectedMatches += selectedMatch ? 1 : 0;
                    choiceMatches += choiceMatch ? 1 : 0;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", item.path("id").asText());
                    row.put("group", item.path("group").asText());
                    row.put("request", item.path("request").asText());
                    row.put("expected", item.path("expected"));
                    row.put("selected", selected);
                    row.put("selectedMatch", selectedMatch);
                    row.put("choiceOnly", choice);
                    row.put("choiceOnlyMatch", choiceMatch);
                    row.put("milliseconds", (System.nanoTime() - start) / 1_000_000);
                    row.put("decisions", results);
                    rows.add(row);
                }
            }
            Path output = Path.of(System.getProperty("routing.eval.output", "target/routing-eval.json"));
            Files.createDirectories(output.toAbsolutePath().getParent());
            Map<String, Object> report = Map.of(
                    "cases", cases.size(), "needsFirstMatches", selectedMatches, "choiceOnlyMatches", choiceMatches,
                    "fanOutThreshold", main.getCamelContext().resolvePropertyPlaceholders("{{routing.fan-out-threshold}}"),
                    "minimumChoiceProbability", main.getCamelContext().resolvePropertyPlaceholders("{{routing.minimum-probability}}"),
                    "results", rows);
            JSON.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);
            System.out.printf("Routing evaluation: needs-first %d/%d, Choice-only %d/%d. Report: %s%n",
                    selectedMatches, cases.size(), choiceMatches, cases.size(), output);
        } finally {
            main.stop();
        }
    }

    private boolean matches(List<String> selected, JsonNode alternatives) {
        Set<String> actual = Set.copyOf(selected);
        for (JsonNode alternative : alternatives) {
            Set<String> expected = new java.util.HashSet<>();
            alternative.forEach(value -> expected.add(value.asText()));
            if (actual.equals(expected)) {
                return true;
            }
        }
        return false;
    }
}
