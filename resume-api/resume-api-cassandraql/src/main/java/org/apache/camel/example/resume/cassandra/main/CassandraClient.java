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

package org.apache.camel.example.resume.cassandra.main;

import java.net.InetSocketAddress;

import com.datastax.oss.driver.api.core.CqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple client for Cassandra for testing purposes
 */
class CassandraClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraClient.class);

    private CqlSession session;

    public CassandraClient(String host, int port) {
        tryConnect(host, port);
    }

    private void tryConnect(String host, int port) {
        InetSocketAddress socketAddress = new InetSocketAddress(host, port);

        int i = 12;
        do {
            try {
                LOG.info("Trying to connect to: {}:{}", host, port);
                session = CqlSession.builder()
                        .addContactPoint(socketAddress)
                        .withLocalDatacenter("datacenter1")
                        .build();
                return;
            } catch (Exception e) {
                LOG.error("Failed to connect: {}", e.getMessage());
                i--;

                if (i == 0) {
                    throw e;
                }

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                    return;
                }
            }
        } while (i > 0);
    }


    public ExampleDao newExampleDao() {
        return new ExampleDao(this.session);
    }

    @Override
    public void close() {
        session.close();
    }
}
