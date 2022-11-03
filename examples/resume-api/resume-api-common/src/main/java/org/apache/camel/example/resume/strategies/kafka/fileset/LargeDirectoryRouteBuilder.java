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
import org.apache.camel.example.resume.strategies.kafka.KafkaUtil;
import org.apache.camel.resume.ResumeStrategyConfiguration;
import org.apache.camel.resume.ResumeStrategyConfigurationBuilder;
import org.apache.camel.resume.cache.ResumeCache;
import org.apache.camel.support.resume.Resumables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LargeDirectoryRouteBuilder extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(LargeDirectoryRouteBuilder.class);
    private final ResumeCache<File> cache;
    private final ResumeStrategyConfigurationBuilder<? extends ResumeStrategyConfigurationBuilder, ? extends ResumeStrategyConfiguration> resumeStrategyConfigurationBuilder;
    private final long delay;

    public LargeDirectoryRouteBuilder(ResumeCache<File> cache) {
        this(KafkaUtil.getDefaultKafkaResumeStrategyConfigurationBuilder(), cache);
    }

    public LargeDirectoryRouteBuilder(ResumeStrategyConfigurationBuilder<?, ?> resumeStrategyConfigurationBuilder, ResumeCache<File> cache) {
        this(resumeStrategyConfigurationBuilder, cache, 0);
    }

    public LargeDirectoryRouteBuilder(ResumeStrategyConfigurationBuilder<? extends ResumeStrategyConfigurationBuilder, ? extends ResumeStrategyConfiguration> resumeStrategyConfigurationBuilder, ResumeCache<File> cache, long delay) {
        this.resumeStrategyConfigurationBuilder = resumeStrategyConfigurationBuilder;
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
        getCamelContext().getRegistry().bind(ResumeCache.DEFAULT_NAME, cache);

        from("file:{{input.dir}}?noop=true&recursive=true")
                .resumable().configuration(resumeStrategyConfigurationBuilder)
                .process(this::process)
                .to("file:{{output.dir}}");
    }

}
