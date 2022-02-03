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
package org.apache.camel.example.transformer.demo;

import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spi.DataType;
import org.apache.camel.spi.DataTypeAware;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel can transform and validate data.
 */
@CamelSpringTest
@ContextConfiguration("/META-INF/spring/camel-context.xml")
class TransformerTest {

    @Autowired
    ProducerTemplate template;
    @Autowired
    ModelCamelContext context;

    @Test
    void should_transform_and_validate_pojo() {
        NotifyBuilder notify = new NotifyBuilder(context).fromRoute("java")
                .whenCompleted(1).and()
                .whenCompleted(1).wereSentTo("file:target/output*").create();
        // Given
        Order order = new Order()
                .setOrderId("Order-Java-0001")
                .setItemId("MILK")
                .setQuantity(3);
        // When
        OrderResponse response = template.requestBody("direct:java", order, OrderResponse.class);

        // Then
        assertTrue(
            notify.matches(20, TimeUnit.SECONDS), "1 message should be completed"
        );
        assertNotNull(response);
        assertTrue(response.isAccepted());
        assertEquals("Order-Java-0001", response.getOrderId());
        assertEquals(String.format("Order accepted:[item='%s' quantity='%s']", order.getItemId(), order.getQuantity()), response.getDescription());
    }

    @Test
    void should_transform_and_validate_json() {
        NotifyBuilder notify = new NotifyBuilder(context).fromRoute("json")
                .whenCompleted(1).and()
                .whenCompleted(1).wereSentTo("file:target/output*").create();
        // Given
        String orderJson = "{\"orderId\":\"Order-JSON-0001\", \"itemId\":\"MIZUYO-KAN\", \"quantity\":\"16350\"}";
        // When
        Exchange answerJson = template.send("direct:json", ex -> ((DataTypeAware) ex.getIn()).setBody(orderJson, new DataType("json")));

        // Then
        assertTrue(
            notify.matches(20, TimeUnit.SECONDS), "1 message should be completed"
        );
        assertNotNull(answerJson);
        String response = answerJson.getIn().getBody(String.class);
        assertNotNull(response);
        assertTrue(response.contains("\"accepted\":true"));
        assertTrue(response.contains("\"orderId\":\"Order-JSON-0001\""));
        assertTrue(response.contains("Order accepted:[item='MIZUYO-KAN' quantity='16350']"));
    }

    @Test
    void should_transform_and_validate_xml() {
        NotifyBuilder notify = new NotifyBuilder(context).fromRoute("xml")
                .whenCompleted(1).and()
                .whenCompleted(1).wereSentTo("file:target/output*").create();
        // Given
        String orderXml = "<order orderId=\"Order-XML-0001\" itemId=\"MIKAN\" quantity=\"365\"/>";

        // When
        Exchange answerXml = template.send("direct:xml",
            ex -> ((DataTypeAware) ex.getIn()).setBody(orderXml, new DataType("xml:XMLOrder"))
        );

        // Then
        assertTrue(
            notify.matches(20, TimeUnit.SECONDS), "1 message should be completed"
        );
        assertNotNull(answerXml);
        String response = answerXml.getIn().getBody(String.class);
        assertNotNull(response);
        assertTrue(response.contains("accepted=\"true\""));
        assertTrue(response.contains("orderId=\"Order-XML-0001\""));
        assertTrue(response.contains("Order accepted:[item='MIKAN' quantity='365']"));
    }
}
