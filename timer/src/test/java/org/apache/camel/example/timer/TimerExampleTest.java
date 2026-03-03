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
package org.apache.camel.example.timer;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.junit6.CamelTestSupport;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.apache.camel.example.timer.TimerExample.createTimerRoutes;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test verifying that timer routes work correctly.
 */
class TimerExampleTest extends CamelTestSupport {

    @Test
    void should_fire_simple_timer() {
        // Wait for at least 2 messages from the simple timer (fires every 300ms)
        NotifyBuilder notify = new NotifyBuilder(context)
                .from("timer:simple*")
                .whenCompleted(2)
                .create();

        assertTrue(
                notify.matches(20, TimeUnit.SECONDS), "Simple timer should fire at least twice"
        );
    }

    @Test
    void should_respect_repeat_count() {
        // The limited timer should fire exactly 3 times
        NotifyBuilder notify = new NotifyBuilder(context)
                .from("timer:limited*")
                .whenExactlyCompleted(3)
                .create();

        assertTrue(
                notify.matches(20, TimeUnit.SECONDS), "Limited timer should fire exactly 3 times"
        );
    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return createTimerRoutes();
    }
}