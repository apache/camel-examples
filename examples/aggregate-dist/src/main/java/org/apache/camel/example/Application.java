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
package org.apache.camel.example;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import org.apache.camel.processor.aggregate.jdbc.JdbcAggregationRepository;
import org.apache.camel.spi.AggregationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private static final int THREADS = 20;
    private static final int END = 100;

    private static final String CID_HEADER = "corrId";
    private static final String DB_URL = "jdbc:derby:target/testdb;create=true";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "admin";

    private final String correlationId;
    private final String expectedResult;
    private final Queue<Integer> inputQueue;
    private final CountDownLatch latch;
    private final ExecutorService executor;

    public Application() {
        this.correlationId = UUID.randomUUID().toString();
        this.expectedResult = IntStream.rangeClosed(1, END)
                .mapToObj(Integer::toString).collect(Collectors.joining("."));
        this.inputQueue = new ConcurrentLinkedQueue<>();
        this.latch = new CountDownLatch(THREADS);
        this.executor = Executors.newFixedThreadPool(THREADS);
    }

    public boolean launch() {
        try {
            // init
            IntStream.rangeClosed(1, END).forEach(inputQueue::add);

            // test
            for (int i = 0; i < THREADS; i++) {
                executor.execute(this::startCamel);
            }

            // wait
            latch.await();
            stop();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("The test has been interrupted", e);
        }
        return false;
    }

    public Future<Boolean> asyncLaunch() {
        return CompletableFuture.supplyAsync(this::launch);
    }

    public static void main(String[] args) {
        new Application().launch();
    }

    private void startCamel() {
        try {
            Main camel = new Main();
            camel.configure().addRoutesBuilder(new RouteBuilder() {
                @Override
                public void configure() {
                    from("timer:foo?repeatCount=1&period=1")
                            .setExchangePattern(ExchangePattern.InOnly)
                            .bean(new MyProducerBean());

                    from("direct:aggregator")
                            .filter(body().isNotNull())
                            .aggregate().header(CID_HEADER)
                            .aggregationStrategy(Application.this::aggregationStrategy)
                            .completionPredicate(Application.this::completionPredicate)
                            .aggregationRepository(getAggregationRepository())
                            .optimisticLocking()
                            .log(LoggingLevel.INFO, "Result: ${body}");
                }
            });

            camel.start();
            LOG.debug("Camel started");
            latch.await();
            camel.stop();
            LOG.debug("Camel stopped");
        } catch (Exception e) {
            LOG.error("Failed to start Camel: {}", e.getMessage());
        }
    }

    private static AggregationRepository getAggregationRepository() {
        SingleConnectionDataSource ds = new SingleConnectionDataSource(DB_URL, DB_USER, DB_PASS, true);
        ds.setAutoCommit(false);
        try (Connection conn = ds.getConnection();
             Statement statement = conn.createStatement()){
            statement.execute(
                    "create table aggregation("
                            + "id varchar(255) not null primary key,"
                            + "exchange blob not null,"
                            + "version bigint not null"
                            + ")");
            statement.execute(
                    "create table aggregation_completed("
                            + "id varchar(255) not null primary key,"
                            + "exchange blob not null,"
                            + "version bigint not null"
                            + ")");
        } catch (SQLException e) {
            if (!e.getMessage().contains("already exists")) {
                LOG.error("Database initialization failure", e);
            }
        }
        DataSourceTransactionManager txManager = new DataSourceTransactionManager(ds);
        // repositoryName (aggregation) must match tableName (aggregation, aggregation_completed)
        JdbcAggregationRepository repo = new JdbcAggregationRepository(txManager, "aggregation", ds);
        repo.setUseRecovery(false);
        repo.setStoreBodyAsText(false);
        return repo;
    }

    private Exchange aggregationStrategy(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            return newExchange;
        }
        String body = oldExchange.getIn().getBody(String.class) + "."
                + newExchange.getIn().getBody(String.class);
        oldExchange.getIn().setBody(body);
        LOG.trace("Queue: {}", inputQueue);
        LOG.trace("Aggregation: {}", oldExchange.getIn().getBody());
        return oldExchange;
    }

    private boolean completionPredicate(Exchange exchange) {
        boolean isComplete = false;
        final String body = exchange.getIn().getBody(String.class);
        if (body != null && !body.isEmpty()) {
            String[] a1 = body.split("\\.");
            String[] a2 = expectedResult.split("\\.");
            if (a1.length == a2.length) {
                Arrays.sort(a1);
                Arrays.sort(a2);
                isComplete = Arrays.equals(a1, a2);
            }
        }
        LOG.debug("Complete? {}", isComplete);
        return isComplete;
    }

    private void stop() {
        try {
            executor.shutdown();
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Termination interrupted");
        } finally {
            if (executor.isTerminated()) {
                LOG.debug("All tasks completed");
            } else {
                LOG.error("Forcing shutdown of tasks");
                executor.shutdownNow();
            }
        }
    }

    class MyProducerBean {
        public void run(Exchange exchange) {
            CamelContext context = exchange.getContext();
            try (ProducerTemplate template = context.createProducerTemplate()) {
                template.setThreadedAsyncMode(false);
                Endpoint endpoint = context.getEndpoint("direct:aggregator");
                Integer item;
                while ((item = inputQueue.poll()) != null) {
                    template.sendBodyAndHeader(endpoint, item, CID_HEADER, correlationId);
                }
                template.stop();
            } catch (IOException e) {
                LOG.error("Error during execution");
            }
            latch.countDown();
        }
    }

}
