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

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.manager.view.DesignDocument;
import com.couchbase.client.java.manager.view.View;
import com.couchbase.client.java.view.DesignDocumentNamespace;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.couchbase.CouchbaseConstants;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.camel.util.PropertiesHelper.asProperties;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel consume data from Couchbase.
 */
class CouchbaseTest extends CamelTestSupport {

    private static final String IMAGE = "couchbase/server:7.0.3";
    private static final String BUCKET = "test-bucket-" + System.currentTimeMillis();
    private static CouchbaseContainer CONTAINER;
    private static Cluster CLUSTER;

    @BeforeAll
    static void init() {
        CONTAINER = new CouchbaseContainer(IMAGE) {
            {
                // Camel component tries to use the default port of the KV Service, so we need to fix it
                final int kvPort = 11210;
                addFixedExposedPort(kvPort, kvPort);
            }
        }.withBucket(new BucketDefinition(BUCKET));
        CONTAINER.start();
        CLUSTER = Cluster.connect(
            CONTAINER.getConnectionString(),
            CONTAINER.getUsername(),
            CONTAINER.getPassword()
        );
        DesignDocument designDoc = new DesignDocument(
            CouchbaseConstants.DEFAULT_DESIGN_DOCUMENT_NAME,
            Collections.singletonMap(
                CouchbaseConstants.DEFAULT_VIEWNAME,
                new View("function (doc, meta) {  emit(meta.id, doc);}")
            )
        );
        CLUSTER.bucket(BUCKET).viewIndexes().upsertDesignDocument(designDoc, DesignDocumentNamespace.PRODUCTION);
    }

    @AfterAll
    static void destroy() {
        if (CONTAINER != null) {
            try {
                if (CLUSTER != null) {
                    CLUSTER.disconnect();
                }
            } finally {
                CONTAINER.stop();
            }
        }
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        camelContext.getPropertiesComponent().setLocation("classpath:application.properties");
        return camelContext;
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        return asProperties(
            "couchbase.host", CONTAINER.getHost(),
            "couchbase.port", Integer.toString(CONTAINER.getBootstrapHttpDirectPort()),
            "couchbase.username", CONTAINER.getUsername(),
            "couchbase.password", CONTAINER.getPassword(),
            "couchbase.bucket", BUCKET
        );
    }

    @Test
    void should_consume_bucket() {
        Bucket bucket = CLUSTER.bucket(BUCKET);
        bucket.waitUntilReady(Duration.ofSeconds(10L));
        for (int i = 0; i < 10; i++) {
            bucket.defaultCollection().upsert("my-doc-" + i, JsonObject.create().put("name", "My Name " + i));
        }

        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(10).wereSentTo("log:info").create();
        assertTrue(
            notify.matches(20, TimeUnit.SECONDS), "10 messages should be completed"
        );
    }

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new MyRouteBuilder();
    }
}
