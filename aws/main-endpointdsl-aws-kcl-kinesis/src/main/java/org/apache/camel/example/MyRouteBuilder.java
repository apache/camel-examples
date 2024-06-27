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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.component.aws2.kinesis.Kinesis2Constants;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

/**
 * To use the endpoint DSL then we must extend EndpointRouteBuilder instead of RouteBuilder
 */
public class MyRouteBuilder extends EndpointRouteBuilder {

    @Override
    public void configure() throws Exception {

        from(aws2Kinesis("{{streamName}}").useDefaultCredentialsProvider(true).advanced().useKclConsumers(true).asyncClient(true))
            .log("The content is ${body} from ${headers.CamelAwsKinesisShardId}").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        ByteBuffer buffer = exchange.getMessage().getBody(ByteBuffer.class);
                        buffer.flip();
                        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer);
                        exchange.getMessage().setBody(charBuffer.toString());
                    }
                }).to(file("./target/kcl-files/")).startupOrder(1);

        from(timer("kcl-ingest-1").repeatCount(5))
                .setBody(constant("Camel KCL Test Partition 1"))
                .setHeader(Kinesis2Constants.PARTITION_KEY, constant("partition-1"))
                .to(aws2Kinesis("{{streamName}}").useDefaultCredentialsProvider(true)).startupOrder(2);

        from(timer("kcl-ingest-2").repeatCount(5))
                .setBody(constant("Camel KCL Test Partition 2"))
                .setHeader(Kinesis2Constants.PARTITION_KEY, constant("partition-2"))
                .to(aws2Kinesis("{{streamName}}").useDefaultCredentialsProvider(true)).startupOrder(3);

        from(timer("kcl-ingest-3").repeatCount(5))
                .setBody(constant("Camel KCL Test Partition 3"))
                .setHeader(Kinesis2Constants.PARTITION_KEY, constant("partition-3"))
                .to(aws2Kinesis("{{streamName}}").useDefaultCredentialsProvider(true)).startupOrder(4);
    }
}
