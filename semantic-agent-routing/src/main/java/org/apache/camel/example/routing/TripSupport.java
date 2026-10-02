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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stateless decision helpers for specialist selection and reply handling.
 *
 * <p>Methods accept ordinary Java values, leave their inputs unchanged, and return
 * specialist names or action labels. The caller handles routing, response formatting,
 * generation, and retry execution.
 */
public class TripSupport {

    /**
     * Selects every specialist whose need flag is {@link Boolean#TRUE}, in reservation,
     * weather, then cost order. Several specialists may be selected for one request.
     *
     * <p>If no need flag is true, the selected Choice label is used only when its
     * probability is present and meets the inclusive minimum. Provider confidence
     * does not affect this fallback.
     *
     * @param decisions the decision map: {@code needsReservation}, {@code needsWeather},
     *                  and {@code needsCost} contain Boolean flags; {@code specialist}
     *                  contains the selected Choice label as a String
     * @param choiceProbabilities probabilities keyed by specialist name
     * @param minimumProbability inclusive minimum probability for the Choice fallback
     * @return a new mutable list of specialist names, or an empty list when the caller
     *         should ask the customer to clarify the request
     */
    public List<String> selectSpecialists(
            Map<String, Object> decisions, Map<String, Double> choiceProbabilities, double minimumProbability) {
        List<String> specialists = new ArrayList<>();
        if (Boolean.TRUE.equals(decisions.get("needsReservation"))) {
            specialists.add("reservation");
        }
        if (Boolean.TRUE.equals(decisions.get("needsWeather"))) {
            specialists.add("weather");
        }
        if (Boolean.TRUE.equals(decisions.get("needsCost"))) {
            specialists.add("cost");
        }

        if (specialists.isEmpty()) {
            String selected = (String) decisions.get("specialist");
            Double probability = choiceProbabilities.get(selected);
            if (probability != null && probability >= minimumProbability) {
                specialists.add(selected);
            }
        }
        return specialists;
    }

    /**
     * Classifies the collected specialist replies before preparing the final response.
     * Any status other than {@code accepted}, including a missing status, takes
     * precedence over the number of replies and requires review.
     *
     * @param replies a nonempty list of reply maps, each containing a {@code status}
     * @return {@code review} if any reply was not accepted, {@code single} for one
     *         accepted reply, or {@code merge} for several accepted replies
     */
    public String replyAction(List<Map<String, Object>> replies) {
        if (replies.stream().anyMatch(reply -> !"accepted".equals(reply.get("status")))) {
            return "review";
        }
        return replies.size() == 1 ? "single" : "merge";
    }

    /**
     * Determines the next action after a specialist or merged draft has been checked.
     * An accepted result always takes precedence. Otherwise, a retry requires a
     * probability strictly below {@code 0.4}, the lower edge of the demo's uncertainty
     * band, and an attempt count below the configured limit. Uncertain results,
     * missing probabilities, and exhausted attempts require review.
     *
     * @param accepted whether the draft passed its semantic check
     * @param probability the check's probability, or {@code null} when unavailable
     * @param attempts generation attempts already made, including the current draft
     * @param maxAttempts maximum generation attempts, including the first attempt
     * @return {@code accept}, {@code retry}, or {@code review}
     */
    public String draftAction(boolean accepted, Double probability, int attempts, int maxAttempts) {
        if (accepted) {
            return "accept";
        }
        if (probability != null && probability < 0.4 && attempts < maxAttempts) {
            return "retry";
        }
        return "review";
    }
}
