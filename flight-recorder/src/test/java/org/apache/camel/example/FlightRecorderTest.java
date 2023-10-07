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

import org.apache.camel.main.Main;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A unit test allowing to check that the flight recorder is launched as expected.
 */
class FlightRecorderTest {

    @Test
    void should_launch_flight_recorder() throws Exception {
        long before = Files.list(Paths.get(".")).filter(p -> p.toString().endsWith(".jfr")).count();
        // use Camels Main class
        Main main = new Main(FlightRecorderTest.class);
        try {
            main.start();
        } finally {
            main.stop();
        }
        long after = Files.list(Paths.get(".")).filter(p -> p.toString().endsWith(".jfr")).count();
        assertEquals(1L, after - before, "A flight recorder file should have been created");
    }

}
