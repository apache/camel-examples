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

package org.apache.camel.example.resume.fileset.clusterized.strategies;

import java.io.File;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.resume.Resumables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterizedLargeDirectoryRouteBuilder extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterizedLargeDirectoryRouteBuilder.class);

    private void process(Exchange exchange) {
        File path = exchange.getMessage().getHeader("CamelFilePath", File.class);
        LOG.debug("Processing {}", path.getPath());
        exchange.getMessage().setHeader(Exchange.OFFSET, Resumables.of(path.getParentFile(), path));

        // Put a delay to simulate slow processing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            LOG.trace("Interrupted while sleeping", e);
        }
    }

    /**
     * Let's configure the Camel routing rules using Java code...
     */
    public void configure() {
        from("timer:heartbeat?period=10000")
                .routeId("heartbeat")
                .log("HeartBeat route (timer) ...");

        from("master:resume-ns:file:{{input.dir}}?noop=true&recursive=true")
                .resumable("testResumeStrategy")
                .routeId("clustered")
                .process(this::process)
                .to("file:{{output.dir}}");
    }

}
