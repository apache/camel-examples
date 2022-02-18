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
package org.apache.camel.example.netty;

import java.util.concurrent.TimeUnit;

import org.apache.camel.Predicate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.main.junit5.CamelMainTestSupport;
import org.junit.jupiter.api.Test;

import static org.apache.camel.example.netty.MyClient.createCorrelationManager;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel can communicate over TCP with Netty using a custom codec.
 */
class NettyTest extends CamelMainTestSupport {

    @Override
    protected void bindToRegistry(Registry registry) throws Exception {
        // Bind the Correlation Manager to use for the test
        registry.bind("myManager", createCorrelationManager());
        // Bind the custom codec
        registry.bind("myEncoder", new MyCodecEncoderFactory());
        registry.bind("myDecoder", new MyCodecDecoderFactory());
    }

    @Test
    void should_exchange_messages_over_tcp_using_a_custom_codec() {
        Predicate isAGeneratedWord = exchange -> exchange.getIn().getBody(String.class).endsWith("-Echo");
        NotifyBuilder notify = new NotifyBuilder(context).fromRoute("client")
            .whenCompleted(5).whenAllDoneMatches(isAGeneratedWord)
            .and().fromRoute("server").whenCompleted(5).whenAllDoneMatches(isAGeneratedWord).create();
        assertTrue(
            notify.matches(20, TimeUnit.SECONDS), "5 messages should be exchanged"
        );
    }

    @Override
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addRoutesBuilder(new MyServer.MyRouteBuilder());
        configuration.addRoutesBuilder(new MyClient.MyRouteBuilder());
    }
}
