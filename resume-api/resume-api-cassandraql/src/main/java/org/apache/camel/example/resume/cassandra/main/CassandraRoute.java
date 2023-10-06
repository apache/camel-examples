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

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cassandra.CassandraConstants;
import org.apache.camel.processor.resume.kafka.KafkaResumeStrategyConfigurationBuilder;
import org.apache.camel.resume.ResumeAction;
import org.apache.camel.support.resume.Resumables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraRoute extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraRoute.class);
    private final CountDownLatch latch;
    private final int batchSize;
    private final CassandraClient client;
    private final KafkaResumeStrategyConfigurationBuilder configurationBuilder;

    public CassandraRoute(CountDownLatch latch, int batchSize, KafkaResumeStrategyConfigurationBuilder configurationBuilder, CassandraClient client) {
        this.latch = latch;
        this.batchSize = batchSize;
        this.configurationBuilder = configurationBuilder;
        this.client = client;
    }

    private class CustomResumeAction implements ResumeAction {
        private final ExampleDao dao;

        public CustomResumeAction(ExampleDao dao) {
            this.dao = dao;
            dao.useKeySpace();
        }

        @Override
        public boolean evalEntry(Object key, Object value) {
            LOG.trace("Evaluating entry {} with value {}", key, value);
            dao.delete(UUID.fromString((String) value));

            return false;
        }
    }

    private void addResumeInfo(Exchange exchange) {
        ExampleEntry bodyEntry = exchange.getMessage().getBody(ExampleEntry.class);

        LOG.info("Received record number: {}", bodyEntry.getNumber());
        exchange.getMessage().setHeader(Exchange.OFFSET, Resumables.of(bodyEntry.getId().toString(), bodyEntry.getId().toString()));
        if (latch.getCount() == 1) {
            exchange.setRouteStop(true);
        }

        latch.countDown();
    }


    @Override
    public void configure() {
        bindToRegistry(CassandraConstants.CASSANDRA_RESUME_ACTION, new CustomResumeAction(client.newExampleDao()));

        fromF("cql:{{cassandra.host}}:{{cassandra.cql3.port}}/camel_ks?cql=%s&resultSetConversionStrategy=#class:%s",
                ExampleDao.getSelectStatement(batchSize), ExampleResultSetConversionStrategy.class.getName())
                .split(body()) // We receive a list of records so, for each
                .resumable()
                .configuration(configurationBuilder)
                    .intermittent(true) // Set to ignore empty data sets that will generate exchanges w/ no offset information
                .process(this::addResumeInfo);

    }
}
