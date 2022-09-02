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

        // Task to check for secret updates
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        CloudTrailAnalyzerTask analyzerTask = new CloudTrailAnalyzerTask(main, "SecretTest");
        executor.scheduleAtFixedRate(analyzerTask, 1, 30, TimeUnit.SECONDS);

        // now keep the application running until the JVM is terminated (ctrl + c or sigterm)
        main.run(args);
    }

    protected static class CloudTrailAnalyzerTask implements Runnable {
        private Main main;
        private String secretName;
        private static String eventSourceSecrets = "secretsmanager.amazonaws.com";

        public CloudTrailAnalyzerTask(Main main, String secretName) {
            this.main = main;
            this.secretName = secretName;
        }

        @Override
        public void run() {
            boolean triggerReloading = false;
            Region regionValue = Region.of(main.getCamelContext().getPropertiesComponent().loadProperties().get("camel.vault.aws.region").toString());
            CloudTrailClient cloudTrailClient = CloudTrailClient.builder()
                    .region(regionValue)
                    .credentialsProvider(ProfileCredentialsProvider.create())
                    .build();
            try {
                LookupEventsRequest.Builder eventsRequestBuilder = LookupEventsRequest.builder()
                        .maxResults(100).lookupAttributes(LookupAttribute.builder().attributeKey(LookupAttributeKey.EVENT_SOURCE).attributeValue(eventSourceSecrets).build());

                if (lastTime != null) {
                    eventsRequestBuilder.startTime(lastTime.plusMillis(1000));
                }

                LookupEventsRequest lookupEventsRequest = eventsRequestBuilder.build();

                LookupEventsResponse response = cloudTrailClient.lookupEvents(lookupEventsRequest);
                List<Event> events = response.events();

                if (events.size() > 0) {
                    lastTime = events.get(0).eventTime();
                }

                LOG.info("Found " + events.size() + " events");
                for (Event event : events) {
                    if (event.eventSource().equalsIgnoreCase(eventSourceSecrets)) {
                        if (event.eventName().equalsIgnoreCase("PutSecretValue")) {
                            List<Resource> a = event.resources();
                            for (Resource res : a) {
                                if (res.resourceName().contains(secretName)) {
                                    LOG.info("Update for secret " + secretName + " detected, triggering a context reload");
                                    triggerReloading = true;
                                    break;
                                }
                            }
                        }
                    }
                }

            } catch (CloudTrailException e) {
                throw e;
            }
            if (triggerReloading) {
                if (main.getCamelContext() != null) {
                    ContextReloadStrategy reload = main.getCamelContext().hasService(ContextReloadStrategy.class);
                    if (reload != null) {
                        // trigger reload
                        reload.onReload(main.getCamelContext().getName());
                    }
                }
            }
        }
    }

}
