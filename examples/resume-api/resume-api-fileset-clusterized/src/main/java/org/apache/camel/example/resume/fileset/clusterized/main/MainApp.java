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

import org.apache.camel.component.zookeeper.cluster.ZooKeeperClusterService;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Camel Application
 */
public class MainApp {
    private static final Logger LOG = LoggerFactory.getLogger(MainApp.class);


    /**
     * A main() so we can easily run these routing rules in our IDE
     */
    public static void main(String... args) throws Exception {
        Main main = new Main();

        ZooKeeperClusterService clusterService = new ZooKeeperClusterService();

        // This identifies the camel instance in the cluster
        String nodeId = System.getProperty("resume.example.node.id");

        // This property points to our zookeeper host
        String nodeHost = System.getProperty("resume.example.zk.host");

        clusterService.setId(nodeId);
        clusterService.setNodes(nodeHost);
        clusterService.setBasePath("/camel/cluster");

        /* In this example we use an implementation of MainListener to configure the services and listen for
           events on Camel's main
         */
        final ClusterizedListener listener = new ClusterizedListener(clusterService);
        main.addMainListener(listener);

        main.run(args);
    }




}

