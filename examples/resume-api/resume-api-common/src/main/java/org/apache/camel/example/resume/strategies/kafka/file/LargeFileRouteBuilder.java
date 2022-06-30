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

package org.apache.camel.example.resume.strategies.kafka.file;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.processor.resume.kafka.KafkaResumeStrategy;
import org.apache.camel.resume.Resumable;
import org.apache.camel.resume.ResumeStrategy;
import org.apache.camel.resume.cache.ResumeCache;
import org.apache.camel.support.resume.Resumables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Camel Java DSL Router
 */
public class LargeFileRouteBuilder extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(LargeFileRouteBuilder.class);

    private KafkaResumeStrategy testResumeStrategy;
    private final ResumeCache<File> cache;

    private long lastOffset;
    private long lineCount = 0;

    private final CountDownLatch latch;

    public LargeFileRouteBuilder(KafkaResumeStrategy resumeStrategy, ResumeCache<File> cache, CountDownLatch latch) {
        this.testResumeStrategy = resumeStrategy;
        this.cache = cache;
        this.latch = latch;
    }

    private void process(Exchange exchange) {
        final String body = exchange.getMessage().getBody(String.class);
        final String filePath = exchange.getMessage().getHeader(Exchange.FILE_PATH, String.class);
        final File file = new File(filePath);

        // Get the initial offset and use it to update the last offset when reading the first line
        final Resumable resumable = exchange.getMessage().getHeader(FileConstants.INITIAL_OFFSET, Resumable.class);
        final Long value = resumable.getLastOffset().getValue(Long.class);

        if (lineCount == 0) {
            lastOffset += value;
        }

        // It sums w/ 1 in order to account for the newline that is removed by readLine
        lastOffset += body.length() + 1;
        lineCount++;

        exchange.getMessage().setHeader(Exchange.OFFSET, Resumables.of(file, lastOffset));

        LOG.info("Read data: {} / offset key: {} / offset value: {}", body, filePath, lastOffset);
        if (latch.getCount() == 1) {
            exchange.setRouteStop(true);
        }

        latch.countDown();
    }

    /**
     * Let's configure the Camel routing rules using Java code...
     */
    public void configure() {
        getCamelContext().getRegistry().bind(ResumeStrategy.DEFAULT_NAME, testResumeStrategy);
        getCamelContext().getRegistry().bind(ResumeCache.DEFAULT_NAME, cache);

        from("file:{{input.dir}}?noop=true&fileName={{input.file}}")
                .routeId("largeFileRoute")
                .convertBodyTo(String.class)
                .split(body().tokenize("\n"))
                    .streaming()
                    .stopOnException()
                .resumable()
                    .resumeStrategy(ResumeStrategy.DEFAULT_NAME)
                    .intermittent(true)
                    .process(this::process);

    }

}
