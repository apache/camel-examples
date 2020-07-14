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
package org.apache.camel.example.salesforce;

import org.apache.camel.builder.RouteBuilder;

public class SalesforceRouteBuilder extends RouteBuilder {

    @Override
    public void configure() {
        from("salesforce:{{salesforce.topic}}")
        .unmarshal().json()
        .choice()
            .when(header("CamelSalesforceEventType").isEqualTo("created"))
                .log("New Salesforce contact was created: [ID:${body[Id]}, Name:${body[Name]}, Email:${body[Email]}, Phone: ${body[Phone]}]")
            .when(header("CamelSalesforceEventType").isEqualTo("updated"))
                .log("A Salesforce contact was updated: [ID:${body[Id]}, Name:${body[Name]}, Email:${body[Email]}, Phone: ${body[Phone]}]")
            .when(header("CamelSalesforceEventType").isEqualTo("undeleted"))
                .log("A Salesforce contact was undeleted: [ID:${body[Id]}, Name:${body[Name]}, Email:${body[Email]}, Phone: ${body[Phone]}]")
            .when(header("CamelSalesforceEventType").isEqualTo("deleted"))
                .log("A Salesforce contact was deleted: [ID:${body[Id]}]");
    }
}
