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

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws2.sqs.Sqs2Component;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.test.main.junit5.CamelMainTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * A unit test checking that Camel can consume messages from Amazon SQS.
 */
class AwsSQSTest extends CamelMainTestSupport {

    private static final String IMAGE = "localstack/localstack:0.13.3";
    private static LocalStackContainer CONTAINER;

    @BeforeAll
    static void init() {
        CONTAINER = new LocalStackContainer(DockerImageName.parse(IMAGE))
                .withServices(SQS)
                .waitingFor(Wait.forLogMessage(".*Ready\\.\n", 1));;
        CONTAINER.start();
    }

    @AfterAll
    static void destroy() {
        if (CONTAINER != null) {
            CONTAINER.stop();
        }
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        Sqs2Component sqs = camelContext.getComponent("aws2-sqs", Sqs2Component.class);
        sqs.getConfiguration().setAmazonSQSClient(
                SqsClient.builder()
                .endpointOverride(CONTAINER.getEndpointOverride(SQS))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(CONTAINER.getAccessKey(), CONTAINER.getSecretKey())
                    )
                )
                .region(Region.of(CONTAINER.getRegion()))
                .build()
        );
        return camelContext;
    }

    @Test
    void should_poll_sqs_queue() {
        // Add a message first
        template.send("direct:publishMessage", exchange -> {
            exchange.getIn().setBody("Camel rocks!");
        });

        NotifyBuilder notify = new NotifyBuilder(context).from("aws2-sqs:*").whenCompleted(1).create();
        assertTrue(
            notify.matches(20, TimeUnit.SECONDS), "1 message should be completed"
        );
    }

    @Override
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addRoutesBuilder(MyRouteBuilder.class);
        configuration.addRoutesBuilder(new PublishMessageRouteBuilder());
    }

    private static class PublishMessageRouteBuilder extends RouteBuilder {

        @Override
        public void configure() throws Exception {
            from("direct:publishMessage").to("aws2-sqs://{{sqs-queue-name}}?autoCreateQueue=true");
        }
    }
}
