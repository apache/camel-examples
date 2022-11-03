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

package org.apache.camel.example.resume.aws.kinesis.main;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.caffeine.resume.CaffeineCache;
import org.apache.camel.main.Main;
import org.apache.camel.test.infra.aws2.clients.AWSSDKClientUtils;
import org.apache.camel.test.infra.aws2.clients.KinesisUtils;
import software.amazon.awssdk.services.kinesis.KinesisClient;

/**
 * A Camel Application
 */
public class MainApp {

    /**
     * A main() so we can easily run these routing rules in our IDE
     */
    public static void main(String... args) {
        Main main = new Main();

        String streamName = System.getProperty("aws.kinesis.streamName", "aws-kinesis-test");

        String action = System.getProperty("resume.action");
        KinesisClient client = AWSSDKClientUtils.newKinesisClient();
        if ("load".equalsIgnoreCase(action)) {
            // do load
            loadData(client, streamName, 500);
            return;
        }

        Integer batchSize = Integer.parseInt(System.getProperty("batch.size", "50"));
        CountDownLatch latch = new CountDownLatch(batchSize);

        Executors.newSingleThreadExecutor().submit(() -> waitForStop(main, latch));

        RouteBuilder routeBuilder = new KinesisRoute(streamName, new CaffeineCache<>(100), client, latch);

        main.configure().addRoutesBuilder(routeBuilder);
        main.start();
    }


    private static void loadData(KinesisClient client, String streamName, int recordCount) {
        KinesisUtils.createStream(client, streamName);
        KinesisUtils.putRecords(client, streamName, recordCount);
    }

    private static void waitForStop(Main main, CountDownLatch latch) {
        try {
            main.stop();

            while (!main.isStopped()) {
                Thread.sleep(1000);
            }

            latch.await();
            System.exit(0);
        } catch (InterruptedException e) {
            System.exit(1);
        }
    }

}

