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

import org.apache.camel.component.aws.secretsmanager.vault.CloudTrailReloadTriggerTask;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.camel.spi.ContextReloadStrategy;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudtrail.model.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main class that boot the Camel application
 */
public final class MyApplication {

    private static final Logger LOG = LoggerFactory.getLogger(MyApplication.class);

    public static Instant lastTime = null;

    private MyApplication() {
    }

    public static void main(String[] args) throws Exception {
        // use Camels Main class
        Main main = new Main(MyApplication.class);

        // now keep the application running until the JVM is terminated (ctrl + c or sigterm)
        main.start();

        // Task to check for secret updates
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        CloudTrailReloadTriggerTask analyzerTask = new CloudTrailReloadTriggerTask(main.getCamelContext(), "SecretTest1");
        executor.scheduleAtFixedRate(analyzerTask, 30, 60, TimeUnit.SECONDS);
    }

}
