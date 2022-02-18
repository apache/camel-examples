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
package org.apache.camel.example.debezium;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws2.kinesis.Kinesis2Component;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.test.main.junit5.CamelMainTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.camel.util.PropertiesHelper.asProperties;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS;

/**
 * A unit test checking that Camel can propagate changes from one Database to another thanks to Debezium.
 */
class DebeziumTest extends CamelMainTestSupport {

    private static final String AWS_IMAGE = "localstack/localstack:0.13.3";
    private static final String PGSQL_IMAGE = "debezium/example-postgres:1.9";
    private static final String CASSANDRA_IMAGE = "cassandra:4.0.1";
    private static LocalStackContainer AWS_CONTAINER;
    private static PostgreSQLContainer<?> PGSQL_CONTAINER;
    private static CassandraContainer<?> CASSANDRA_CONTAINER;

    private static final String SOURCE_DB_NAME = "debezium-db";
    private static final String SOURCE_DB_SCHEMA = "inventory";
    private static final String SOURCE_DB_USERNAME = "pgsql-user";
    private static final String SOURCE_DB_PASSWORD = "pgsql-pw";

    @BeforeAll
    static void init() throws IOException {
        Files.deleteIfExists(Path.of("target/offset-01.data"));
        AWS_CONTAINER = new LocalStackContainer(DockerImageName.parse(AWS_IMAGE))
                .withServices(KINESIS)
                .waitingFor(Wait.forLogMessage(".*Ready\\.\n", 1));
        AWS_CONTAINER.start();
        PGSQL_CONTAINER = new PostgreSQLContainer<>(DockerImageName.parse(PGSQL_IMAGE).asCompatibleSubstituteFor("postgres"))
                .withDatabaseName(SOURCE_DB_NAME)
                .withUsername(SOURCE_DB_USERNAME)
                .withPassword(SOURCE_DB_PASSWORD);
        PGSQL_CONTAINER.start();
        CASSANDRA_CONTAINER = new CassandraContainer<>(CASSANDRA_IMAGE)
                .withInitScript("org/apache/camel/example/debezium/db-init.cql");
        CASSANDRA_CONTAINER.start();
    }

    @AfterAll
    static void destroy() {
        if (AWS_CONTAINER != null) {
            AWS_CONTAINER.stop();
        }
        if (PGSQL_CONTAINER != null) {
            PGSQL_CONTAINER.stop();
        }
        if (CASSANDRA_CONTAINER != null) {
            CASSANDRA_CONTAINER.stop();
        }
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        Kinesis2Component component = camelContext.getComponent("aws2-kinesis", Kinesis2Component.class);
        KinesisClient kinesisClient = KinesisClient.builder()
                .endpointOverride(AWS_CONTAINER.getEndpointOverride(KINESIS))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(AWS_CONTAINER.getAccessKey(), AWS_CONTAINER.getSecretKey())
                        )
                )
                .region(Region.of(AWS_CONTAINER.getRegion()))
                .build();
        // Create the stream
        kinesisClient.createStream(CreateStreamRequest.builder().streamName("camel-debezium-example").shardCount(1).build());
        component.getConfiguration().setAmazonKinesisClient(kinesisClient);
        return camelContext;
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        // Override the host and port of the broker
        return asProperties(
            "debezium.postgres.databaseHostName", PGSQL_CONTAINER.getHost(),
            "debezium.postgres.databasePort", Integer.toString(PGSQL_CONTAINER.getMappedPort(5432)),
            "debezium.postgres.databaseUser", SOURCE_DB_USERNAME,
            "debezium.postgres.databasePassword", SOURCE_DB_PASSWORD,
            "cassandra.host", String.format("%s:%d", CASSANDRA_CONTAINER.getHost(), CASSANDRA_CONTAINER.getMappedPort(9042))
        );
    }

    @Test
    void should_propagate_db_event_thanks_to_debezium() {
        NotifyBuilder notify = new NotifyBuilder(context).from("aws2-kinesis:*").whenCompleted(3).create();

        List<?> resultSource = template.requestBody("direct:select", null, List.class);
        assertEquals(9, resultSource.size(), "We should not have additional products in source");
        await().atMost(20, SECONDS).until(() -> template.requestBody("direct:result", null, List.class).size(), equalTo(0));

        template.sendBody("direct:insert", new Object[] { 1, "scooter", "Small 2-wheel yellow scooter", 5.54 });

        resultSource = template.requestBody("direct:select", null, List.class);
        assertEquals(10, resultSource.size(), "We should have one additional product in source");
        await().atMost(20, SECONDS).until(() -> template.requestBody("direct:result", null, List.class).size(), equalTo(1));

        template.sendBody("direct:update", new Object[] { "yellow scooter", 1 });

        resultSource = template.requestBody("direct:select", null, List.class);
        assertEquals(10, resultSource.size(), "We should not have more product in source");
        await().atMost(20, SECONDS).until(() -> template.requestBody("direct:result", null, List.class).size(), equalTo(1));

        template.sendBody("direct:delete", new Object[] { 1 });

        resultSource = template.requestBody("direct:select", null, List.class);
        assertEquals(9, resultSource.size(), "We should have one less product in source");
        await().atMost(20, SECONDS).until(() -> template.requestBody("direct:result", null, List.class).size(), equalTo(0));

        assertTrue(
            notify.matches(60, SECONDS), "3 messages should be completed"
        );
    }

    @Override
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addRoutesBuilder(DebeziumPgSQLConsumerToKinesis.createRouteBuilder());
        configuration.addRoutesBuilder(KinesisProducerToCassandra.createRouteBuilder());
        configuration.addRoutesBuilder(new ApplyChangesToPgSQLRouteBuilder());
    }

    private static class ApplyChangesToPgSQLRouteBuilder extends RouteBuilder {

        @Override
        public void configure() {
            // required for the sql component
            PGSimpleDataSource db = new PGSimpleDataSource();
            db.setServerNames(new String[]{PGSQL_CONTAINER.getHost()});
            db.setPortNumbers(new int[]{PGSQL_CONTAINER.getMappedPort(5432)});
            db.setUser(SOURCE_DB_USERNAME);
            db.setPassword(SOURCE_DB_PASSWORD);
            db.setDatabaseName(SOURCE_DB_NAME);

            getContext().getComponent("sql", SqlComponent.class).setDataSource(db);
            from("direct:select").toF("sql:select * from %s.products", SOURCE_DB_SCHEMA).to("mock:query");
            from("direct:insert").toF("sql:insert into %s.products (id, name, description, weight) values (#, #, #, #)", SOURCE_DB_SCHEMA).to("mock:insert");
            from("direct:update").toF("sql:update %s.products set name=# where id=#", SOURCE_DB_SCHEMA).to("mock:update");
            from("direct:delete").toF("sql:delete from %s.products where id=#", SOURCE_DB_SCHEMA).to("mock:delete");
            from("direct:result").to("cql://{{cassandra.host}}/{{cassandra.keyspace}}?cql=select * from dbzSink.products").to("mock:result");
        }
    }
}
