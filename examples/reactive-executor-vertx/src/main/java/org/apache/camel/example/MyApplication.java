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

import io.vertx.core.Vertx;
import org.apache.camel.BindToRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.Configuration;
import org.apache.camel.main.Main;
import org.apache.camel.support.LifecycleStrategySupport;

/**
 * Main class that boot the Camel application
 */
public final class MyApplication {

    private MyApplication() {
    }

    public static void main(String[] args) throws Exception {
        // use Camels Main class
        Main main = new Main(MyApplication.class);
        // now keep the application running until the JVM is terminated (ctrl + c or sigterm)
        main.run(args);
    }

    @Configuration
    static class MyConfiguration {

        @BindToRegistry(value = "vertx", beanPostProcess = true)
        public Vertx vertx(CamelContext camelContext) {
            // register existing vertx which should be used by Camel
            final Vertx vertx = Vertx.vertx();
            // register 'vertx' bean stop lifecycle hook
            camelContext.addLifecycleStrategy(new LifecycleStrategySupport() {
                @Override
                public void onContextStopped(CamelContext context) {
                    super.onContextStopped(context);
                    // stop vertx
                    vertx.close();
                }
            });
            return vertx;
        }
    }

}
