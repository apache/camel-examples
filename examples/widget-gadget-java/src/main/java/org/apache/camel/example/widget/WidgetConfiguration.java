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
 *
 */
package org.apache.camel.example.widget;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Configuration;
import org.apache.camel.component.activemq.ActiveMQComponent;

@Configuration
public class WidgetConfiguration {

    @BindToRegistry("activemq")
    public ActiveMQComponent activeMQComponent() {
        ActiveMQComponent amq = new ActiveMQComponent();

        // The ActiveMQ Broker allows anonymous connection by default
        // amq.setUserName("admin");
        // amq.setPassword("admin");

        // the url to the remote ActiveMQ broker
        amq.setBrokerURL("tcp://localhost:61616");

        return amq;
    }
}
