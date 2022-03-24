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
package org.apache.camel.example.widget;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Configuration;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.main.Main;

/**
 * A plain Java Main to start the widget and gadget example.
 */
public final class WidgetMain {

    private WidgetMain() {
        // to comply with checkstyle
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main(WidgetMain.class);
        // start and run Camel (block)
        main.run();
    }
}
