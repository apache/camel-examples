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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

public final class Application {

    public static final String AUTHORIZATION_TOKEN;
    public static final String PHONE_NUMBER_ID;
    public static final String RECIPIENT_PHONE_NUMBER;
    public static final String WEBHOOK_VERIFY_TOKEN;

    static {
        Properties properties = new Properties();
        ClassLoader loader = Application.class.getClassLoader();
        try (InputStream resourceStream = loader.getResourceAsStream("application.properties")) {
            properties.load(resourceStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        AUTHORIZATION_TOKEN = properties.getProperty("authorizationToken");
        PHONE_NUMBER_ID = properties.getProperty("phoneNumberId");
        RECIPIENT_PHONE_NUMBER = properties.getProperty("recipientPhoneNumber");
        WEBHOOK_VERIFY_TOKEN = properties.getProperty("webhookVerifyToken");
    }

    private Application() {
        // noop
    }

    public static void main(String[] args) throws Exception {
        try (CamelContext context = new DefaultCamelContext()) {
            context.start();

            context.addRoutes(new WhatsappRouteBuilder());
            context.addStartupListener(new WhatsappExamplesRunner());
            
            Thread.sleep(6000000);
        }
    }
}
