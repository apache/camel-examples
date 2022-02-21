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

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws2.s3.AWS2S3Component;
import org.apache.camel.component.aws2.s3.AWS2S3Constants;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.test.main.junit5.CamelMainTestSupport;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
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
@Testcontainers
class AwsS3KafkaTest extends CamelMainTestSupport {

    private static final String AWS_IMAGE = "localstack/localstack:0.13.3";
    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:6.2.2";

    @Container
    private final LocalStackContainer awsContainer = new LocalStackContainer(DockerImageName.parse(AWS_IMAGE))
            .withServices(S3)
            .waitingFor(Wait.forLogMessage(".*Ready\\.\n", 1));
    @Container
    private final KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        AWS2S3Component s3 = camelContext.getComponent("aws2-s3", AWS2S3Component.class);
        s3.getConfiguration().setAmazonS3Client(
                S3Client.builder()
                .endpointOverride(awsContainer.getEndpointOverride(S3))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsContainer.getAccessKey(), awsContainer.getSecretKey())
                    )
                )
                .region(Region.of(awsContainer.getRegion()))
                .build()
        );
        return camelContext;
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        return asProperties(
            "kafkaBrokers", String.format("%s:%d", kafkaContainer.getHost(), kafkaContainer.getMappedPort(9093))
        );
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
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addRoutesBuilder(MyRouteBuilder.class);
        configuration.addRoutesBuilder(new AddBucketRouteBuilder());
    }

    private static class AddBucketRouteBuilder extends RouteBuilder {

        @Override
        public void configure() {
            from("direct:putObject").to("aws2-s3://{{bucketName}}?autoCreateBucket=true");
        }
    }
}
