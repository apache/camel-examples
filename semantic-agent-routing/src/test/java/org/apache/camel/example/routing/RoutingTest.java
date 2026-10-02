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

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.camel.main.Main;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoutingTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> SPECIALISTS = List.of("reservation", "weather", "cost", "general");
    private final List<Main> applications = new ArrayList<>();
    private final List<JsonNode> decisions = new CopyOnWriteArrayList<>();
    private final List<JsonNode> completions = new CopyOnWriteArrayList<>();
    private final Map<String, AtomicInteger> generationCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> checkCounts = new ConcurrentHashMap<>();
    private final HttpClient client = HttpClient.newHttpClient();
    private final ExecutorService fixtureThreads = Executors.newCachedThreadPool();
    private HttpServer fixture;
    private String base;
    private Map<String, String> coordinatorProperties;
    private volatile String choice;
    private volatile double confidence;
    private volatile double choiceProbability;
    private volatile Map<String, Double> needs;
    private volatile Map<String, List<Double>> probabilities;
    private volatile String malformedRouting;
    private volatile String malformedCheck;
    private volatile Map<String, String> replies;
    private volatile CountDownLatch parallelCalls;
    private volatile boolean parallelTimedOut;

    @BeforeAll
    void start() throws Exception {
        fixture = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fixture.createContext("/decisions/v2/evaluate", this::decision);
        fixture.createContext("/v1/chat/completions", this::completion);
        fixture.setExecutor(fixtureThreads);
        fixture.start();
        String provider = "http://127.0.0.1:" + fixture.getAddress().getPort();
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("camel.component.openai.api-key", "test-only");
        properties.put("camel.component.openai.base-url", provider + "/v1");
        properties.put("camel.component.openai.model", "fixture-chat-model");
        for (String name : SPECIALISTS) {
            int port = port();
            Map<String, String> agent = new LinkedHashMap<>(properties);
            agent.put("role", name);
            agent.put("port", "" + port);
            application(agent).start();
            properties.put("agents." + name + ".url", "http://127.0.0.1:" + port);
        }
        int port = port();
        properties.put("role", "coordinator");
        properties.put("port", "" + port);
        properties.put("camel.component.typesafe-ai.base-url", provider);
        properties.put("camel.component.typesafe-ai.api-path", "/decisions/v2/evaluate");
        properties.put("camel.component.typesafe-ai.api-key", "fixture-decision-key");
        properties.put("camel.component.typesafe-ai.model", "fixture-decision-model");
        coordinatorProperties = Map.copyOf(properties);
        application(properties).start();
        base = "http://127.0.0.1:" + port;
    }

    private Main application(Map<String, String> properties) {
        Main main = new Main(RoutingApplication.class);
        main.configure().withRoutesIncludePattern("classpath:routes/{{role}}.yaml");
        properties.forEach(main::addOverrideProperty);
        applications.add(main);
        return main;
    }

    private int port() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @AfterAll
    void stop() {
        for (Main main : applications.reversed()) {
            main.stop();
        }
        if (fixture != null) {
            fixture.stop(0);
        }
        fixtureThreads.shutdownNow();
        client.close();
    }

    @BeforeEach
    void reset() {
        decisions.clear();
        completions.clear();
        generationCounts.clear();
        checkCounts.clear();
        choice = "weather";
        confidence = 0.9;
        choiceProbability = 0.85;
        needs = Map.of("weather", 0.9);
        probabilities = Map.of();
        malformedRouting = null;
        malformedCheck = null;
        replies = Map.of();
        parallelCalls = null;
        parallelTimedOut = false;
    }

    @Test
    void routesToEachRealA2AConsumerWithOneBatchAndNoMergeForOneSpecialist() throws Exception {
        Map<String, String> facts = Map.of(
                "reservation", "Read-only demo reservation R-100: compact car, Paris, 12-15 October 2026, confirmed.",
                "weather", "Demo forecast (not live): Lisbon, Tuesday, light rain, 18 C.",
                "cost", "A five-day SUV rental totals 375 EUR.",
                "general", "Demo rental desk hours: Monday-Friday 09:00-18:00.");
        for (String name : SPECIALISTS) {
            reset();
            choice = name;
            needs = name.equals("general") ? Map.of() : Map.of(name, 0.9);
            JsonNode response = trip("Help with " + name);
            assertEquals("accepted", response.path("status").asText());
            assertEquals(List.of(name), JSON.convertValue(response.path("specialists"), List.class));
            assertEquals(1, result(response, name).path("attempts").asInt());
            assertEquals(0, response.path("mergeAttempts").asInt());
            assertEquals(2, decisions.size());
            assertEquals(4, decisions.getFirst().path("questions").size());
            assertTrue(decisions.getFirst().path("questions").has("needsWeather"));
            assertEquals(1, completions.size());
            assertEquals("fixture-chat-model", completions.getFirst().path("model").asText());
            assertEquals(0.3, completions.getFirst().path("temperature").asDouble());
            JsonNode messages = completions.getFirst().path("messages");
            assertEquals("system", messages.get(0).path("role").asText());
            assertTrue(messages.get(0).path("content").asText().contains("the " + name + " specialist"));
            assertTrue(messages.get(0).path("content").asText().contains(facts.get(name)));
            assertTrue(messages.get(0).path("content").asText().contains("within your specialty"));
            assertEquals("Help with " + name, messages.get(1).path("content").asText());
            assertEquals(name, decisions.get(1).path("state").path("specialist").asText());
        }
    }

    @Test
    void confidentChoiceCannotHideMultipleIntentsAndBranchesRunInParallel() throws Exception {
        choice = "reservation";
        confidence = 0.99;
        choiceProbability = 0.99;
        needs = Map.of("weather", 0.91, "cost", 0.85);
        parallelCalls = new CountDownLatch(2);
        JsonNode response = trip("Will it rain in Lisbon and how much is an SUV for five days?");
        assertEquals("accepted", response.path("status").asText());
        assertEquals(List.of("weather", "cost"), JSON.convertValue(response.path("specialists"), List.class));
        assertFalse(parallelTimedOut, "Specialists must enter generation concurrently");
        assertEquals(3, completions.size());
        assertEquals("merge reply 1", response.path("reply").asText());
        assertEquals(1, response.path("mergeAttempts").asInt());
        JsonNode merge = completions.stream().filter(c -> role(c).equals("merge")).findFirst().orElseThrow();
        JsonNode mergeInput = JSON.readTree(merge.path("messages").get(1).path("content").asText());
        assertEquals(2, mergeInput.path("specialistReplies").size());
        assertEquals(Set.of("specialist", "status", "attempts", "reply"),
                JSON.convertValue(mergeInput.path("specialistReplies").get(0), Map.class).keySet());
        assertEquals("weather", mergeInput.path("specialistReplies").get(0).path("specialist").asText());
        assertEquals("cost", mergeInput.path("specialistReplies").get(1).path("specialist").asText());
        assertEquals(4, decisions.size());
        assertEquals(1, decisions.stream().filter(d -> d.path("questions").path("question").path("instructions").asText().contains("final reply")).count());
        for (String name : List.of("weather", "cost")) {
            assertEquals(1, result(response, name).path("attempts").asInt());
        }
    }

    @Test
    void fallsBackToChoiceProbabilityRatherThanProviderConfidence() throws Exception {
        needs = Map.of();
        choice = "general";
        confidence = 0.01;
        assertEquals("accepted", trip("Hello!").path("status").asText());
        reset();
        needs = Map.of();
        confidence = 0.99;
        choiceProbability = 0.549;
        JsonNode response = trip("Can you help me?");
        assertEquals("clarification", response.path("status").asText());
        assertTrue(response.path("specialists").isEmpty());
        assertTrue(response.path("results").isEmpty());
        assertTrue(completions.isEmpty());
        choiceProbability = 0.55;
        assertEquals("accepted", trip("Will it rain?").path("status").asText());
    }

    @Test
    void independentNeedsUseAnInclusiveThreshold() throws Exception {
        needs = Map.of("weather", 0.5, "cost", 0.499);
        choice = "general";
        JsonNode response = trip("Will it rain?");
        assertEquals(List.of("weather"), JSON.convertValue(response.path("specialists"), List.class));
    }

    @Test
    void retriesOnlyTheRejectedSpecialistAndKeepsAttemptCountsIndependent() throws Exception {
        needs = Map.of("weather", 0.9, "cost", 0.9);
        probabilities = Map.of("weather", List.of(0.1, 0.9));
        JsonNode response = trip("Weather and price?");
        assertEquals("accepted", response.path("status").asText());
        assertEquals(2, result(response, "weather").path("attempts").asInt());
        assertEquals(1, result(response, "cost").path("attempts").asInt());
        assertEquals(1, response.path("mergeAttempts").asInt());
        assertEquals(2, generationCounts.get("weather").get());
        assertEquals(1, generationCounts.get("cost").get());
        JsonNode retry = completions.stream().filter(c -> role(c).equals("weather")
                && c.path("messages").get(1).path("content").asText().contains("Previous reply:")).findFirst().orElseThrow();
        assertTrue(retry.path("messages").get(1).path("content").asText().startsWith("Weather and price?\n\n"));
        for (JsonNode decision : decisions) {
            if (decision.path("state").has("specialist")) {
                assertEquals("Weather and price?", decision.path("state").path("request").asText());
            }
        }
    }

    @Test
    void singleSpecialistMustCoverEveryRequestedItemInItsRole() throws Exception {
        needs = Map.of("cost", 0.9);
        probabilities = Map.of("cost", List.of(0.1, 0.9));
        JsonNode response = trip("What are the compact and SUV daily prices?");
        assertEquals("accepted", response.path("status").asText());
        assertEquals(List.of("cost"), JSON.convertValue(response.path("specialists"), List.class));
        assertEquals(2, result(response, "cost").path("attempts").asInt());
        assertEquals(0, response.path("mergeAttempts").asInt());
        String instructions = decisions.get(1).path("questions").path("question").path("instructions").asText();
        assertTrue(instructions.contains("every requested part within the named specialist's"));
        assertTrue(instructions.contains("must not omit items"));
        assertEquals("cost reply 2", response.path("reply").asText());
    }

    @Test
    void completenessRetryDoesNotCallSpecialistsAgain() throws Exception {
        needs = Map.of("weather", 0.9, "cost", 0.9);
        probabilities = Map.of("merge", List.of(0.1, 0.9));
        JsonNode response = trip("Weather and price?");
        assertEquals("accepted", response.path("status").asText());
        assertEquals(2, response.path("mergeAttempts").asInt());
        assertEquals(1, generationCounts.get("weather").get());
        assertEquals(1, generationCounts.get("cost").get());
        assertEquals("merge reply 2", response.path("reply").asText());
        assertTrue(completions.getLast().path("messages").get(1).path("content").asText().contains("omitted part"));
    }

    @Test
    void uncertainSpecialistNeedsReviewWithoutMergingOrReturningItsDraft() throws Exception {
        for (double probability : List.of(0.4, 0.5, 0.6)) {
            reset();
            needs = Map.of("weather", 0.9, "cost", 0.9);
            probabilities = Map.of("weather", List.of(probability));
            JsonNode response = trip("Weather and price?");
            assertEquals("review", response.path("status").asText());
            assertEquals(2, completions.size());
            assertEquals(0, response.path("mergeAttempts").asInt());
            assertFalse(response.toString().contains("weather reply"));
        }
    }

    @Test
    void uncertainMergedReplyNeedsReviewWithoutReturningItsDraft() throws Exception {
        needs = Map.of("weather", 0.9, "cost", 0.9);
        probabilities = Map.of("merge", List.of(0.5));
        JsonNode response = trip("Weather and price?");
        assertEquals("review", response.path("status").asText());
        assertEquals(1, response.path("mergeAttempts").asInt());
        assertFalse(response.toString().contains("merge reply"));
    }

    @Test
    void boundsSpecialistAndMergeRetries() throws Exception {
        probabilities = Map.of("weather", List.of(0.1));
        JsonNode response = trip("Weather?");
        assertEquals("review", response.path("status").asText());
        assertEquals(3, result(response, "weather").path("attempts").asInt());
        assertEquals(3, completions.size());
        reset();
        needs = Map.of("weather", 0.9, "cost", 0.9);
        probabilities = Map.of("merge", List.of(0.1));
        response = trip("Weather and price?");
        assertEquals("review", response.path("status").asText());
        assertEquals(3, response.path("mergeAttempts").asInt());
        assertEquals(5, completions.size());
    }

    @Test
    void readsSelectionThresholdsAndRetryLimitFromProperties() throws Exception {
        Main main = configuredCoordinator(Map.of("routing.fan-out-threshold", "0.8",
                "routing.minimum-probability", "0.8", "routing.max-attempts", "2"));
        try {
            needs = Map.of("weather", 0.75);
            choiceProbability = 0.75;
            assertEquals("clarification", JSON.readTree(postTo(main, "Weather?").body()).path("status").asText());
            assertTrue(completions.isEmpty());
            needs = Map.of("weather", 0.85);
            probabilities = Map.of("weather", List.of(0.1));
            JsonNode response = JSON.readTree(postTo(main, "Weather?").body());
            assertEquals("review", response.path("status").asText());
            assertEquals(2, result(response, "weather").path("attempts").asInt());
        } finally {
            main.stop();
        }
    }

    @Test
    void malformedBatchFailsAtomicallyBeforeCallingAnySpecialist() throws Exception {
        for (String mode : List.of("missing", "extra", "invalid")) {
            reset();
            malformedRouting = mode;
            HttpResponse<String> response = post("Weather?");
            assertEquals(502, response.statusCode());
            assertEquals("application/json", response.headers().firstValue("Content-Type").orElseThrow());
            assertEquals("error", JSON.readTree(response.body()).path("status").asText());
            assertTrue(completions.isEmpty());
        }
    }

    @Test
    void failedSpecialistCheckIsAnErrorWithoutMergeOrRegeneration() throws Exception {
        needs = Map.of("weather", 0.9, "cost", 0.9);
        malformedCheck = "weather";
        HttpResponse<String> response = post("Weather and price?");
        assertEquals(502, response.statusCode());
        assertEquals("error", JSON.readTree(response.body()).path("status").asText());
        assertFalse(generationCounts.containsKey("merge"));
        assertEquals(1, generationCounts.get("weather").get());
        assertFalse(response.body().contains("Exception"));
    }

    @Test
    void failedCompletenessCheckDoesNotRegenerate() throws Exception {
        needs = Map.of("weather", 0.9, "cost", 0.9);
        malformedCheck = "merge";
        assertEquals(502, post("Weather and price?").statusCode());
        assertEquals(1, generationCounts.get("merge").get());
    }

    @Test
    void preservesLiteralTextAndJsonTypes() throws Exception {
        String request = "Weather in \"Lisbon\", please?\nIt's ${body}; café \\ rain.";
        String reply = "Demo: \"rain\", 18 C.\nIt's ${exchangeProperty.specialist}; café \\ weather.";
        replies = Map.of("weather", reply);
        JsonNode response = trip(request);
        assertEquals(reply, response.path("reply").asText());
        assertEquals(request, decisions.get(1).path("state").path("request").asText());
        assertEquals(reply, decisions.get(1).path("state").path("reply").asText());
        assertTrue(response.path("mergeAttempts").isIntegralNumber());
        JsonNode result = result(response, "weather");
        assertTrue(result.path("attempts").isIntegralNumber());
        assertEquals(Set.of("status", "specialists", "results", "mergeAttempts", "reply"),
                JSON.convertValue(response, Map.class).keySet());
        assertEquals(Set.of("specialist", "status", "attempts", "reply"), JSON.convertValue(result, Map.class).keySet());
    }

    @Test
    void emptySpecialistAndMergeRepliesFailWithoutCheckingOrRetrying() throws Exception {
        replies = Map.of("weather", " \t\n");
        assertEquals(502, post("Weather?").statusCode());
        assertEquals(1, decisions.size());
        assertEquals(1, completions.size());
        reset();
        needs = Map.of("weather", 0.9, "cost", 0.9);
        replies = Map.of("merge", " \t\n");
        assertEquals(502, post("Weather and price?").statusCode());
        assertEquals(3, decisions.size());
        assertEquals(3, completions.size());
    }

    @Test
    void concurrentRequestsKeepSeparateAttemptsAndOriginalRequests() throws Exception {
        needs = Map.of("weather", 0.9, "cost", 0.9);
        var first = client.sendAsync(request(base, "First request: weather and price?"), HttpResponse.BodyHandlers.ofString());
        var second = client.sendAsync(request(base, "Second request: weather and price?"), HttpResponse.BodyHandlers.ofString());
        for (var future : List.of(first, second)) {
            HttpResponse<String> response = future.get(20, TimeUnit.SECONDS);
            assertEquals(200, response.statusCode());
            JsonNode body = JSON.readTree(response.body());
            assertEquals("accepted", body.path("status").asText());
            assertEquals(2, body.path("results").size());
            assertEquals(1, body.path("mergeAttempts").asInt());
            for (JsonNode result : body.path("results")) {
                assertEquals(1, result.path("attempts").asInt());
            }
        }
        Set<String> requests = Set.of("First request: weather and price?", "Second request: weather and price?");
        assertEquals(2, completions.stream().filter(c -> role(c).equals("merge"))
                .map(c -> c.path("messages").get(1).path("content").asText())
                .filter(prompt -> requests.stream().filter(prompt::contains).count() == 1).count());
    }

    @Test
    void routingCanBeEvaluatedWithoutA2AOrGenerationServices() throws Exception {
        Map<String, String> properties = new LinkedHashMap<>(coordinatorProperties);
        properties.put("camel.server.enabled", "false");
        properties.put("camel.component.openai.api-key", "{{env:ROUTING_TEST_UNUSED_KEY:}}");
        properties.put("camel.component.openai.base-url", "http://127.0.0.1:1");
        for (String name : SPECIALISTS) {
            properties.put("agents." + name + ".url", "http://127.0.0.1:1");
        }
        Main main = application(properties);
        main.configure().withRouteFilterIncludePattern("select-specialists");
        try {
            main.start();
            try (var producer = main.getCamelContext().createProducerTemplate()) {
                var exchange = producer.request("direct:select-specialists", e -> e.getMessage().setBody("Weather?"));
                if (exchange.getException() != null) {
                    throw exchange.getException();
                }
                assertEquals(List.of("weather"), exchange.getProperty("specialists"));
                assertEquals(Set.of("specialist", "needsReservation", "needsWeather", "needsCost"),
                        exchange.getProperty("CamelSemanticResults", Map.class).keySet());
                assertEquals(1, decisions.size());
                assertTrue(completions.isEmpty());
            }
        } finally {
            main.stop();
        }
    }

    @Test
    void acceptsTheMaximumRequestLength() throws Exception {
        assertEquals("accepted", trip("x".repeat(4000)).path("status").asText());
    }

    @Test
    void invalidInputNeverReachesAModel() throws Exception {
        for (String request : List.of("", " ", "\t\n", "\u2003", "x".repeat(4001))) {
            HttpResponse<String> response = post(request);
            assertEquals(400, response.statusCode());
            assertEquals("application/json", response.headers().firstValue("Content-Type").orElseThrow());
            assertEquals("invalid_request", JSON.readTree(response.body()).path("status").asText());
        }
        assertTrue(decisions.isEmpty());
        assertTrue(completions.isEmpty());
    }

    private JsonNode result(JsonNode response, String specialist) {
        for (JsonNode result : response.path("results")) {
            if (result.path("specialist").asText().equals(specialist)) {
                return result;
            }
        }
        throw new AssertionError("Missing specialist result: " + specialist);
    }

    private JsonNode trip(String request) throws Exception {
        HttpResponse<String> response = post(request);
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElseThrow());
        return JSON.readTree(response.body());
    }

    private Main configuredCoordinator(Map<String, String> overrides) throws Exception {
        Map<String, String> properties = new LinkedHashMap<>(coordinatorProperties);
        properties.put("port", "" + port());
        properties.putAll(overrides);
        Main main = application(properties);
        main.start();
        return main;
    }

    private HttpResponse<String> postTo(Main main, String request) throws Exception {
        String port = main.getCamelContext().resolvePropertyPlaceholders("{{port}}");
        return client.send(request("http://127.0.0.1:" + port, request), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String request) throws Exception {
        return client.send(request(base, request), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest request(String endpoint, String request) {
        return HttpRequest.newBuilder(URI.create(endpoint + "/trip")).timeout(java.time.Duration.ofSeconds(20))
                .header("Content-Type", "text/plain").POST(HttpRequest.BodyPublishers.ofString(request)).build();
    }

    private void decision(HttpExchange exchange) throws java.io.IOException {
        if (!"Bearer fixture-decision-key".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
        }
        JsonNode request = JSON.readTree(exchange.getRequestBody());
        decisions.add(request);
        Map<String, Object> answers = new LinkedHashMap<>();
        if (request.path("questions").has("specialist")) {
            Map<String, Double> scores = new LinkedHashMap<>();
            for (String candidate : SPECIALISTS) {
                scores.put(candidate, candidate.equals(choice) ? choiceProbability : (1 - choiceProbability) / 3);
            }
            answers.put("specialist", Map.of("type", "choice", "choice", choice, "confidence", confidence, "probabilities", scores));
            for (String name : List.of("reservation", "weather", "cost")) {
                String question = "needs" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                answers.put(question, Map.of("type", "noul", "noul", needs.getOrDefault(name, 0.1)));
            }
            if ("missing".equals(malformedRouting)) {
                answers.remove("needsCost");
            } else if ("extra".equals(malformedRouting)) {
                answers.put("extra", Map.of("type", "noul", "noul", 0.9));
            } else if ("invalid".equals(malformedRouting)) {
                answers.put("needsCost", Map.of("type", "noul", "noul", 1.5));
            }
        } else {
            String question = request.path("questions").fieldNames().next();
            String role = request.path("questions").path(question).path("instructions").asText().contains("final reply")
                    ? "merge" : request.path("state").path("specialist").asText();
            if (!role.equals(malformedCheck)) {
                String key = role + ":" + request.path("state").path("request").asText();
                int attempt = checkCounts.computeIfAbsent(key, k -> new AtomicInteger()).getAndIncrement();
                List<Double> scores = probabilities.getOrDefault(role, List.of(0.9));
                answers.put(question, Map.of("type", "noul", "noul", scores.get(Math.min(attempt, scores.size() - 1))));
            }
        }
        send(exchange, Map.of("model", request.path("model").asText(), "usage", Map.of(), "answers", answers));
    }

    private String role(JsonNode completion) {
        String prompt = completion.path("messages").get(0).path("content").asText();
        return SPECIALISTS.stream().filter(name -> prompt.contains("the " + name + " specialist")).findFirst().orElse("merge");
    }

    private void completion(HttpExchange exchange) throws java.io.IOException {
        if (!"Bearer test-only".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return;
        }
        JsonNode completion = JSON.readTree(exchange.getRequestBody());
        completions.add(completion);
        String role = role(completion);
        int attempt = generationCounts.computeIfAbsent(role, k -> new AtomicInteger()).incrementAndGet();
        if (parallelCalls != null && !role.equals("merge")) {
            parallelCalls.countDown();
            try {
                if (!parallelCalls.await(5, TimeUnit.SECONDS)) {
                    parallelTimedOut = true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.IOException(e);
            }
        }
        send(exchange, Map.of("id", "test", "object", "chat.completion", "created", 1, "model", "fixture-chat-model",
                "choices", List.of(Map.of("index", 0, "finish_reason", "stop", "message",
                        Map.of("role", "assistant", "content", replies.getOrDefault(role, role + " reply " + attempt)))),
                "usage", Map.of("prompt_tokens", 1, "completion_tokens", 1, "total_tokens", 2)));
    }

    private void send(HttpExchange exchange, Object body) throws java.io.IOException {
        byte[] bytes = JSON.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
