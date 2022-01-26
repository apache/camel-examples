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

import org.apache.camel.CamelContext;
import org.apache.camel.Predicate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.apache.camel.example.netty.MyClient.createCorrelationManager;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel can communicate over TCP with Netty using a custom codec.
 */
class NettyTest extends CamelTestSupport {

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        // Bind the Correlation Manager to use for the test
        camelContext.getRegistry().bind("myManager", createCorrelationManager());
        // Bind the custom codec
        camelContext.getRegistry().bind("myEncoder", new MyCodecEncoderFactory());
        camelContext.getRegistry().bind("myDecoder", new MyCodecDecoderFactory());
        return camelContext;
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
    protected RoutesBuilder[] createRouteBuilders() {
        return new RoutesBuilder[]{new MyServer.MyRouteBuilder(), new MyClient.MyRouteBuilder()};
    }
}
