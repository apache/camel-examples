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

package org.apache.camel.example.resume.strategies.kafka.fileset;

import java.io.File;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.resume.ResumeStrategy;
import org.apache.camel.resume.cache.ResumeCache;
import org.apache.camel.support.resume.Resumables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LargeDirectoryRouteBuilder extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(LargeDirectoryRouteBuilder.class);
    private final ResumeStrategy resumeStrategy;
    private final ResumeCache<File> cache;
    private final long delay;

    public LargeDirectoryRouteBuilder(ResumeStrategy resumeStrategy, ResumeCache<File> cache) {
        this(resumeStrategy, cache, 0);
    }

    public LargeDirectoryRouteBuilder(ResumeStrategy resumeStrategy, ResumeCache<File> cache, long delay) {
        this.resumeStrategy = resumeStrategy;
        this.cache = cache;
        this.delay = delay;
    }

    private void process(Exchange exchange) throws InterruptedException {
        File path = exchange.getMessage().getHeader("CamelFilePath", File.class);
        LOG.debug("Processing {}", path.getPath());
        exchange.getMessage().setHeader(Exchange.OFFSET, Resumables.of(path.getParentFile(), path));

        if (delay > 0) {
            Thread.sleep(delay);
        }
    }

    /**
     * Let's configure the Camel routing rules using Java code...
     */
    public void configure() {
        getCamelContext().getRegistry().bind(ResumeStrategy.DEFAULT_NAME, resumeStrategy);
        getCamelContext().getRegistry().bind(ResumeCache.DEFAULT_NAME, cache);

        from("file:{{input.dir}}?noop=true&recursive=true&preSort=true")
                .resumable(ResumeStrategy.DEFAULT_NAME)
                .process(this::process)
                .to("file:{{output.dir}}");
    }

}
