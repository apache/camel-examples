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
package org.apache.camel.example;

import java.util.Map;

import org.apache.camel.health.HealthCheckResultBuilder;
import org.apache.camel.impl.health.AbstractHealthCheck;

/**
 * A chaos monkey health check that reports UP or DOWN in a chaotic way.
 *
 * This is a custom implementation of a Camel {@link org.apache.camel.health.HealthCheck}
 * which is automatic discovered if bound in the {@link org.apache.camel.spi.Registry} and
 * used as part of Camel's health-check system.
 */
public class MonkeyHealthCheck extends AbstractHealthCheck {

    private boolean up = true;

    protected MonkeyHealthCheck() {
        super("custom", "monkey");
    }

    @Override
    protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
        builder.detail("monkey", "The chaos monkey was here");
         if (up) {
             builder.up();
         } else {
             builder.down();
         }
    }

    public String chaos() {
        up = !up;
        return up ? "All is okay" : "Chaos monkey was here";
    }

}
