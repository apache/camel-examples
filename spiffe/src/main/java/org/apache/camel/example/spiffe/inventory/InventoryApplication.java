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
package org.apache.camel.example.spiffe.inventory;

import org.apache.camel.example.spiffe.IdentityRoutes;
import org.apache.camel.example.spiffe.policy.WorkloadIdentityPolicy;
import org.apache.camel.main.Main;

/**
 * Boots the inventory: the second hop of the example, an HTTP API that only the backend may call.
 */
public final class InventoryApplication {

    private InventoryApplication() {
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        // the embedded HTTP server
        main.configure().httpServer().withEnabled(true).withPort(8080);
        // the very same policy as the backend, configured for this service
        main.configure().addRoutesBuilder(new WorkloadIdentityPolicy("inventory"));
        main.configure().addRoutesBuilder(InventoryRoutes.class);
        main.configure().addRoutesBuilder(IdentityRoutes.class);
        // now keep the application running until the JVM is terminated (ctrl + c or sigterm)
        main.run(args);
    }
}
