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

import java.util.Random;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Configuration;
import org.apache.camel.ExchangeTimedOutException;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.netty.DefaultChannelHandlerFactory;
import org.apache.camel.main.Main;

/**
 * Netty client which calls the server every half-second with a random word.
 */
public final class MyClient {

    private MyClient() {
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main(MyClient.class);

        main.run(args);
    }

    @Configuration
    public static class MyClientConfiguration {

        @BindToRegistry("myEncoder")
        public DefaultChannelHandlerFactory myCodecEncoderFactory() {
            return new MyCodecEncoderFactory();
        }

        @BindToRegistry("myDecoder")
        public DefaultChannelHandlerFactory myCodecDecoderFactory() {
            return new MyCodecDecoderFactory();
        }

        @BindToRegistry("myManager")
        public MyCorrelationManager myCorrelationManager() {
            return createCorrelationManager();
        }
    }

    static MyCorrelationManager createCorrelationManager() {
        // setup correlation manager and its timeout (when a request has not received a response within the given time millis)
        MyCorrelationManager manager = new MyCorrelationManager();
        // set timeout for each request message that did not receive a reply message
        manager.setTimeout(5000);
        // set the logging level when a timeout was hit, ny default its DEBUG
        manager.setTimeoutLoggingLevel(LoggingLevel.INFO);
        return manager;
    }

    public static class MyRouteBuilder extends RouteBuilder {

        private static final String[] WORDS = new String[]{"foo", "bar", "baz", "beer", "wine", "cheese"};
        private int counter;
        private final Random random = new Random();

        public int increment() {
            return ++counter;
        }

        public String word() {
            return WORDS[random.nextInt(WORDS.length)];
        }

        @Override
        public void configure() throws Exception {
            // lets build a special custom error message for timeout
            onException(ExchangeTimedOutException.class)
                // here we tell Camel to continue routing
                .continued(true)
                // after it has built this special timeout error message body
                .setBody(simple("#${header.corId}:${header.word}-Time out error!!!"));

            from("timer:trigger").id("client")
                // set correlation id as unique incrementing number
                .setHeader("corId", method(this, "increment"))
                // set random word to use in request
                .setHeader("word", method(this, "word"))
                // build request message as a string body
                .setBody(simple("#${header.corId}:${header.word}"))
                // log request before
                .log("Request:  ${id}:${body}")
                // call netty server using a single shared connection and using custom correlation manager
                // to ensure we can correctly map the request and response pairs
                .to("netty:tcp://localhost:4444?sync=true&encoders=#bean:myEncoder&decoders=#bean:myDecoder"
                    + "&producerPoolEnabled=false&correlationManager=#bean:myManager")
                // log response after
                .log("Response: ${id}:${body}");
        }
    }
}
