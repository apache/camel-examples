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
package org.apache.camel.example.spiffe.backend;

import org.apache.camel.example.spiffe.IdentityRoutes;
import org.apache.camel.example.spiffe.policy.WorkloadIdentityPolicy;
import org.apache.camel.main.Main;

/**
 * Boots the backend: an HTTP API for orders that only serves callers with a valid JWT-SVID, and that calls the
 * inventory service with its own identity.
 */
public final class BackendApplication {

    private BackendApplication() {
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        // the embedded HTTP server
        main.configure().httpServer().withEnabled(true).withPort(8080);
        // the workload identity policy of this service, and the routes that use it
        WorkloadIdentityPolicy policy = new WorkloadIdentityPolicy("backend");
        main.configure().addRoutesBuilder(policy);
        main.configure().addRoutesBuilder(new BackendRoutes(policy.getAuditTrail()));
        main.configure().addRoutesBuilder(IdentityRoutes.class);
        // now keep the application running until the JVM is terminated (ctrl + c or sigterm)
        main.run(args);
    }
}
