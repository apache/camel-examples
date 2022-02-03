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
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws2.s3.AWS2S3Component;
import org.apache.camel.component.aws2.s3.AWS2S3Constants;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import static org.apache.camel.util.PropertiesHelper.asProperties;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

/**
 * A unit test checking that Camel can poll an Amazon S3 bucket and put the data into a Kafka topic.
 */
class AwsS3KafkaTest extends CamelTestSupport {

    private static final String AWS_IMAGE = "localstack/localstack:0.13.3";
    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:6.2.2";
    private static LocalStackContainer AWS_CONTAINER;
    private static KafkaContainer KAFKA_CONTAINER;

    @BeforeAll
    static void init() {
        AWS_CONTAINER = new LocalStackContainer(DockerImageName.parse(AWS_IMAGE))
                .withServices(S3)
                .waitingFor(Wait.forLogMessage(".*Ready\\.\n", 1));;
        AWS_CONTAINER.start();
        KAFKA_CONTAINER = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));
        KAFKA_CONTAINER.start();
    }

    @AfterAll
    static void destroy() {
        if (AWS_CONTAINER != null) {
            AWS_CONTAINER.stop();
        }
        if (KAFKA_CONTAINER != null) {
            KAFKA_CONTAINER.stop();
        }
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        // Set the location of the configuration
        camelContext.getPropertiesComponent().setLocation("classpath:application.properties");
        AWS2S3Component s3 = camelContext.getComponent("aws2-s3", AWS2S3Component.class);
        s3.getConfiguration().setAmazonS3Client(
                S3Client.builder()
                .endpointOverride(AWS_CONTAINER.getEndpointOverride(S3))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(AWS_CONTAINER.getAccessKey(), AWS_CONTAINER.getSecretKey())
                    )
                )
                .region(Region.of(AWS_CONTAINER.getRegion()))
                .build()
        );
        // Override the host and port of the broker
        camelContext.getPropertiesComponent().setOverrideProperties(
            asProperties(
                "kafkaBrokers", String.format("%s:%d", KAFKA_CONTAINER.getHost(), KAFKA_CONTAINER.getMappedPort(9093))
            )
        );
        return camelContext;
    }

    @Test
    void should_poll_s3_bucket_and_push_to_kafka() {
        // Add a bucket first
        template.send("direct:putObject", exchange -> {
            exchange.getIn().setHeader(AWS2S3Constants.KEY, "camel-content-type.txt");
            exchange.getIn().setHeader(AWS2S3Constants.CONTENT_TYPE, "application/text");
            exchange.getIn().setBody("Camel rocks!");
        });

        NotifyBuilder notify = new NotifyBuilder(context).from("kafka:*").whenCompleted(1).create();
        assertTrue(
            notify.matches(20, TimeUnit.SECONDS), "1 message should be completed"
        );
    }

    @Override
    protected RoutesBuilder[] createRouteBuilders() {
        return new RoutesBuilder[]{new MyRouteBuilder(), new AddBucketRouteBuilder()};
    }

    private static class AddBucketRouteBuilder extends RouteBuilder {

        @Override
        public void configure() {
            from("direct:putObject").to("aws2-s3://{{bucketName}}?autoCreateBucket=true");
        }
    }
}
