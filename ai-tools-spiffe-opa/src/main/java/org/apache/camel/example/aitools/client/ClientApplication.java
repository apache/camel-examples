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
package org.apache.camel.example.aitools.client;

import org.apache.camel.example.aitools.IdentityRoutes;
import org.apache.camel.main.Main;

/**
 * Boots a caller of the assistant: it authenticates with the JWT-SVIDs it gets from the SPIFFE Workload API and sends
 * a natural-language request.
 * <p>
 * Both the public chatbot and the support console of this example run this same class. Each ends up with a different
 * SPIFFE ID because it runs as a different Unix user, which the SPIRE agent maps to a different registration entry.
 */
public final class ClientApplication {

    private ClientApplication() {
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.configure().addRoutesBuilder(new ClientRoutes());
        main.configure().addRoutesBuilder(IdentityRoutes.class);
        // now keep the application running until the JVM is terminated (ctrl + c or sigterm)
        main.run(args);
    }
}
