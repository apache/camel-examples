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
package org.apache.camel.example.whatsapp;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.whatsapp.model.TextMessage;
import org.apache.camel.component.whatsapp.model.TextMessageRequest;

public class WhatsappRouteBuilder extends RouteBuilder {

    @Override
    public void configure() {
        restConfiguration().port(8080);

        // Send custom message
        from("direct:start")
            .toF("whatsapp:%s/?authorizationToken=%s", Application.PHONE_NUMBER_ID, Application.AUTHORIZATION_TOKEN);

        // Echo to a message sent from the number
        fromF("webhook:whatsapp:%s?authorizationToken=%s&webhookVerifyToken=%s", Application.PHONE_NUMBER_ID, Application.AUTHORIZATION_TOKEN, Application.WEBHOOK_VERIFY_TOKEN)
            .log("${body}")
            .choice().when().jsonpath("$.entry[0].changes[0].value.messages", true)
            .setHeader("CamelWhatsappBody").jsonpath("$.entry[0].changes[0].value.messages[0].text.body")
            .setHeader("CamelWhatsappSentMessage").jsonpath("$.entry[0].changes[0].value.contacts[0].profile.name")
            .setHeader("CamelWhatsappPhoneNumber").jsonpath("$.entry[0].changes[0].value.contacts[0].wa_id")
            .process(exchange -> {
                TextMessageRequest request = new TextMessageRequest();
                request.setTo(exchange.getIn().getHeader("CamelWhatsappPhoneNumber").toString());
                request.setText(new TextMessage());
                request.getText().setBody(String.format("Hello %s message sent was %s", 
                                                        exchange.getIn().getHeader("CamelWhatsappSentMessage"),
                                                        exchange.getIn().getHeader("CamelWhatsappBody")));
                
                exchange.getIn().setBody(request);
            })
            .toF("whatsapp:%s/?authorizationToken=%s", Application.PHONE_NUMBER_ID, Application.AUTHORIZATION_TOKEN)
        .end();
    }
}
