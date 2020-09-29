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

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.dsl.EventbridgeEndpointBuilderFactory.EventbridgeOperations;
import org.apache.camel.component.aws2.eventbridge.EventbridgeConstants;

import software.amazon.awssdk.services.eventbridge.model.Target;

/**
 * To use the endpoint DSL then we must extend EndpointRouteBuilder instead of RouteBuilder
 */
public class MyRouteBuilder extends EndpointRouteBuilder {

    @Override
    public void configure() throws Exception {

        from(timer("fire").repeatCount("1"))
        .setHeader(EventbridgeConstants.RULE_NAME, constant("s3-events-rule"))
        .to(aws2Eventbridge("default")
        		.operation(EventbridgeOperations.putRule)
        		.eventPatternFile("file:src/main/resources/eventpattern.json"))
        .process(new Processor() {

            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getIn().setHeader(EventbridgeConstants.RULE_NAME, "s3-events-rule");
                Target target = Target.builder().id("sqs-queue").arn("arn:aws:sqs:eu-west-1:780410022472:camel-connector-test")
                        .build();
                List<Target> targets = new ArrayList<Target>();
                targets.add(target);
                exchange.getIn().setHeader(EventbridgeConstants.TARGETS, targets);
            }
        })
        .to(aws2Eventbridge("default")
        		.operation(EventbridgeOperations.putTargets))
        .log("All set, enjoy!");
    }
}
