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
package org.apache.camel.example.basic;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

/**
 * A basic example running as public static void main.
 */
public final class CamelBasic {

    public static void main(String[] args) throws Exception {
        // create a CamelContext
        try (CamelContext camel = new DefaultCamelContext()) {

            // add routes which can be inlined as anonymous inner class
            // (to keep all code in a single java file for this basic example)
            camel.addRoutes(createBasicRoute());

            // start is not blocking
            camel.start();

            // so run for 10 seconds
            Thread.sleep(10_000);
        }
    }

    static RouteBuilder createBasicRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("timer:foo")
                        .log("Hello Camel");
            }
        };
    }
}
