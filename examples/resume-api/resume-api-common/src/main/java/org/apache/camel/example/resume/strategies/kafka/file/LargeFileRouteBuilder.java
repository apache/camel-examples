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

import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.consumer.GenericFileResumeAdapter;
import org.apache.camel.processor.resume.kafka.KafkaResumeStrategy;
import org.apache.camel.resume.Resumable;
import org.apache.camel.resume.Resumables;
import org.apache.camel.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Camel Java DSL Router
 */
public class LargeFileRouteBuilder extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(LargeFileRouteBuilder.class);

    private KafkaResumeStrategy testResumeStrategy;
    private long lastOffset;
    private int batchSize;
    private final CountDownLatch latch;

    public LargeFileRouteBuilder(KafkaResumeStrategy resumeStrategy, CountDownLatch latch) {
        this.testResumeStrategy = resumeStrategy;
        String tmp = System.getProperty("resume.batch.size", "30");
        this.batchSize = Integer.valueOf(tmp);

        this.latch = latch;
    }

    private void process(Exchange exchange) throws Exception {
        Reader reader = exchange.getIn().getBody(Reader.class);
        BufferedReader br = IOHelper.buffered(reader);

        File path = exchange.getMessage().getHeader("CamelFilePath", File.class);
        LOG.debug("Path: {} ", path);

        final GenericFileResumeAdapter adapter = testResumeStrategy.getAdapter(GenericFileResumeAdapter.class);
        lastOffset = adapter.getLastOffset(path).orElse(0L);
        LOG.debug("Starting to read at offset {}", lastOffset);

        String line = br.readLine();
        int count = 0;
        while (count < batchSize) {
            if (line == null || line.isEmpty()) {
                LOG.debug("End of file");
                // EOF, therefore reset the offset
                final Resumable<File, Long> resumable = Resumables.of(path, 0L);
                exchange.getMessage().setHeader(Exchange.OFFSET, resumable);

                break;
            }

            LOG.debug("Read line at offset {} from the file: {}", lastOffset, line);
            testResumeStrategy.updateLastOffset(Resumables.of(path, lastOffset));

            // It sums w/ 1 in order to account for the newline that is removed by readLine
            lastOffset += line.length() + 1;
            // Simulate slow processing
            Thread.sleep(50);
            count++;

            line = br.readLine();
        }

        if (count == batchSize) {
            exchange.setRouteStop(true);
            latch.countDown();
        }
    }

    /**
     * Let's configure the Camel routing rules using Java code...
     */
    public void configure() {
        getCamelContext().getRegistry().bind("testResumeStrategy", testResumeStrategy);

        from("file:{{input.dir}}?noop=true&fileName={{input.file}}")
                .resumable("testResumeStrategy")
                .routeId("largeFileRoute")
                .convertBodyTo(Reader.class)
                .process(this::process);

    }

}
