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
import org.apache.camel.test.infra.aws.common.services.AWSService;
import org.apache.camel.test.infra.aws2.clients.AWSSDKClientUtils;
import org.apache.camel.test.infra.aws2.services.AWSServiceFactory;
import org.apache.camel.test.main.junit5.CamelMainTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel can consume messages from Amazon SQS.
 */
class AwsSQSTest extends CamelMainTestSupport {

    @RegisterExtension
    private static final AWSService AWS_SERVICE = AWSServiceFactory.createSQSService();

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        Sqs2Component sqs = camelContext.getComponent("aws2-sqs", Sqs2Component.class);
        sqs.getConfiguration().setAmazonSQSClient(AWSSDKClientUtils.newSQSClient());
        return camelContext;
    }

    @Test
    void should_poll_sqs_queue() {
        // Add a message first
        template.send("direct:publishMessage", exchange -> exchange.getIn().setBody("Camel rocks!"));

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
