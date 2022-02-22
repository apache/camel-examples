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
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.test.infra.aws.common.services.AWSService;
import org.apache.camel.test.infra.aws2.clients.AWSSDKClientUtils;
import org.apache.camel.test.infra.aws2.services.AWSServiceFactory;
import org.apache.camel.test.infra.kafka.services.KafkaService;
import org.apache.camel.test.infra.kafka.services.KafkaServiceFactory;
import org.apache.camel.test.main.junit5.CamelMainTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.apache.camel.util.PropertiesHelper.asProperties;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel can poll data from a Kafka topic and put it into an Amazon S3 bucket.
 */
class KafkaAwsS3Test extends CamelMainTestSupport {

    @RegisterExtension
    private static final AWSService AWS_SERVICE = AWSServiceFactory.createS3Service();
    @RegisterExtension
    private static final KafkaService KAFKA_SERVICE = KafkaServiceFactory.createService();

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        AWS2S3Component s3 = camelContext.getComponent("aws2-s3", AWS2S3Component.class);
        s3.getConfiguration().setAmazonS3Client(AWSSDKClientUtils.newS3Client());
        return camelContext;
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        return asProperties(
            "kafkaBrokers", KAFKA_SERVICE.getBootstrapServers()
        );
    }

    @Test
    void should_poll_kafka_and_push_to_s3_bucket() {
        NotifyBuilder notify = new NotifyBuilder(context).from("kafka:*").whenCompleted(1).create();
        // Load data into Kafka
        template.sendBody("direct:kafka", "Camel rocks in topic 1!");
        assertTrue(
            notify.matches(10, TimeUnit.SECONDS), "2 messages should be completed"
        );
    }

    @Override
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addRoutesBuilder(MyRouteBuilder.class);
        configuration.addRoutesBuilder(new LoadKafkaRouteBuilder());
    }

    private static class LoadKafkaRouteBuilder extends RouteBuilder {

        @Override
        public void configure() {
            from("direct:kafka").to("kafka:{{kafkaTopic1}}?brokers={{kafkaBrokers}}");
        }
    }
}
