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
package org.apache.camel.example.pojo;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CamelContextTest extends CamelSpringTestSupport {

    @Override
    protected Path testDirectory() {
        return Paths.get("target/messages");
    }

    @Test
    void testCheckFiles() throws Exception {
        // wait a little for the files to be picked up and processed
        Thread.sleep(5000);

        File file = new File("target/messages/emea/hr_pickup");
        assertTrue(file.exists(), "The pickup folder should exists");
        assertEquals(1, file.list().length, "There should be 1 dumped files");
        file = new File("target/messages/amer/hr_pickup");
        assertTrue(file.exists(), "The pickup folder should exists");
        assertEquals(2, file.list().length, "There should be 2 dumped files");
    }

    @Override
    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("META-INF/spring/camel-context.xml");
    }

}
