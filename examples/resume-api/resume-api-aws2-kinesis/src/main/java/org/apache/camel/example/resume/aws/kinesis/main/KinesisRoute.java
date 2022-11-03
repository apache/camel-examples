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

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws2.kinesis.Kinesis2Constants;
import org.apache.camel.example.resume.strategies.kafka.KafkaUtil;
import org.apache.camel.resume.cache.ResumeCache;
import org.apache.camel.support.resume.Resumables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.kinesis.KinesisClient;

public class KinesisRoute extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(KinesisRoute.class);

    private final String streamName;
    private final ResumeCache<String> resumeCache;
    private final KinesisClient client;
    private final CountDownLatch latch;

    public KinesisRoute(String streamName, ResumeCache<String> resumeCache, KinesisClient client, CountDownLatch latch) {
        this.streamName = streamName;
        this.resumeCache = resumeCache;
        this.client = client;
        this.latch = latch;
    }

    private void addResumeOffset(Exchange exchange) {
        String sequenceNumber = exchange.getMessage().getHeader(Kinesis2Constants.SEQUENCE_NUMBER, String.class);
        exchange.getMessage().setHeader(Exchange.OFFSET, Resumables.of(streamName, sequenceNumber));

        String body = exchange.getMessage().getBody(String.class);
        LOG.warn("Processing: {} with sequence number {}", body, sequenceNumber);
        latch.countDown();
    }

    @Override
    public void configure() {
        bindToRegistry(ResumeCache.DEFAULT_NAME, resumeCache);
        bindToRegistry("amazonKinesisClient", client);

        String kinesisEndpointUri = "aws2-kinesis://%s?amazonKinesisClient=#amazonKinesisClient";

        fromF(kinesisEndpointUri, streamName)
                .resumable().configuration(KafkaUtil.getDefaultKafkaResumeStrategyConfigurationBuilder())
                .process(this::addResumeOffset);
    }
}
