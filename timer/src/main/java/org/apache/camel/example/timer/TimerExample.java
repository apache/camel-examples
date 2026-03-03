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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

/**
 * An example showing how to use the Timer component in various configurations.
 * <p>
 * The timer component is used to generate message exchanges at regular intervals.
 * This is useful for polling, scheduling, and triggering periodic tasks.
 */
public final class TimerExample {

    public static void main(String[] args) throws Exception {
        // create a CamelContext
        try (CamelContext camel = new DefaultCamelContext()) {

            // add routes demonstrating different timer patterns
            camel.addRoutes(createTimerRoutes());

            // start is not blocking
            camel.start();

            // run for 5 seconds to demonstrate the timers
            Thread.sleep(5_000);

            System.out.println("\nShutting down...");
        }
    }

    static RouteBuilder createTimerRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() {

                // Simple timer - fires every 300ms
                from("timer:simple?period=300")
                        .routeId("simple-timer")
                        .setBody(simple("Simple timer fired at ${date:now:yyyy-MM-dd HH:mm:ss}"))
                        .log("${body}");

                // Timer with initial delay - waits 500ms before first fire, then every 300ms
                from("timer:withDelay?delay=500&period=300")
                        .routeId("delayed-timer")
                        .setBody(simple("Delayed timer fired at ${date:now:yyyy-MM-dd HH:mm:ss}"))
                        .log("${body}");

                // Timer with repeat count - fires only 3 times
                from("timer:limited?period=100&repeatCount=3")
                        .routeId("limited-timer")
                        .setBody(simple("Limited timer fired (${header.CamelTimerCounter} of 3) at ${date:now:yyyy-MM-dd HH:mm:ss}"))
                        .log("${body}");

                // Fixed rate timer - uses scheduleAtFixedRate (start-to-start timing)
                // Attempts to maintain fixed intervals. If processing time < period, fires at regular intervals.
                // vs fixedRate=false (default) - uses scheduleWithFixedDelay (end-to-start timing)
                // Waits for completion + full period before next execution.
                from("timer:fixedRate?period=500&fixedRate=true")
                        .routeId("fixed-rate-timer")
                        .setBody(simple("Fixed-rate timer fired at ${date:now:yyyy-MM-dd HH:mm:ss}"))
                        .log("${body}")
                        // simulate some processing time
                        .process(exchange -> {
                            // This 50ms delay is less than the 500ms period, demonstrating fixed-rate timing
                            Thread.sleep(50);
                        });
            }
        };
    }
}