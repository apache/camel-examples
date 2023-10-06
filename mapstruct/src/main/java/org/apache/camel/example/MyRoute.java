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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.example.model.Beer;
import org.apache.camel.example.model.Beverage;
import org.apache.camel.model.rest.RestBindingMode;

public class MyRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // use json binding in rest service
        restConfiguration()
                .host("localhost")
                .port(8080)
                .bindingMode(RestBindingMode.json);

        rest("/beer")
            .post()
                .type(Beer.class)
                .outType(Beverage.class)
                .to("direct:beer");

        from("direct:beer")
                // use type converter which triggers mapstruct to map from Beer to Beverage
                .convertBodyTo(Beverage.class)
                .bean(MyBeanEnricher.class);

    }
}
