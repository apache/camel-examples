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
package org.apache.camel.example.resume.fileset.clusterized.main;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.zookeeper.cluster.ZooKeeperClusterService;
import org.apache.camel.example.resume.fileset.clusterized.strategies.ClusterizedLargeDirectoryRouteBuilder;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.main.MainListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Acts upon events on the main class
 */
class ClusterizedListener implements MainListener {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterizedListener.class);
    private final ZooKeeperClusterService clusterService;

    public ClusterizedListener(ZooKeeperClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @Override
    public void beforeInitialize(BaseMainSupport main) {
        // NO-OP
    }

    // This is called after the CamelContext is created, so we use it to access the context and configure the services
    @Override
    public void beforeConfigure(BaseMainSupport main) {

        try {
            LOG.trace("Adding the cluster service");
            main.getCamelContext().addService(clusterService);

            LOG.trace("Creating the strategy");

            LOG.trace("Creating the route");
            RouteBuilder routeBuilder = new ClusterizedLargeDirectoryRouteBuilder();
            main.getCamelContext().addRoutes(routeBuilder);
        } catch (Exception e) {
            LOG.error("Unable to add the cluster service: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    @Override
    public void afterConfigure(BaseMainSupport main) {
    }

    @Override
    public void beforeStart(BaseMainSupport main) {
    }

    @Override
    public void afterStart(BaseMainSupport main) {
    }

    @Override
    public void beforeStop(BaseMainSupport main) {
    }

    @Override
    public void afterStop(BaseMainSupport main) {
//        main.shutdown();
//        System.exit(0);
    }
}
