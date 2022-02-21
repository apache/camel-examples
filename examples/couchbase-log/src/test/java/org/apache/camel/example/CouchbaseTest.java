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

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.manager.view.DesignDocument;
import com.couchbase.client.java.manager.view.View;
import com.couchbase.client.java.view.DesignDocumentNamespace;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.couchbase.CouchbaseConstants;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.test.main.junit5.CamelMainTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.apache.camel.util.PropertiesHelper.asProperties;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel consume data from Couchbase.
 */
@Testcontainers
class CouchbaseTest extends CamelMainTestSupport {

    private static final String IMAGE = "couchbase/server:7.0.3";
    private static final String BUCKET = "test-bucket-" + System.currentTimeMillis();

    @Container
    private final CouchbaseContainer container = new CouchbaseContainer(IMAGE) {
        {
            // Camel component tries to use the default port of the KV Service, so we need to fix it
            final int kvPort = 11210;
            addFixedExposedPort(kvPort, kvPort);
        }
    }.withBucket(new BucketDefinition(BUCKET));
    private Cluster cluster;

    @BeforeEach
    void init() {
        cluster = Cluster.connect(
            container.getConnectionString(),
            container.getUsername(),
            container.getPassword()
        );
        DesignDocument designDoc = new DesignDocument(
            CouchbaseConstants.DEFAULT_DESIGN_DOCUMENT_NAME,
            Collections.singletonMap(
                CouchbaseConstants.DEFAULT_VIEWNAME,
                new View("function (doc, meta) {  emit(meta.id, doc);}")
            )
        );
        cluster.bucket(BUCKET).viewIndexes().upsertDesignDocument(designDoc, DesignDocumentNamespace.PRODUCTION);
    }

    @AfterEach
    void destroy() {
        if (cluster != null) {
            cluster.disconnect();
        }
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        return asProperties(
            "couchbase.host", container.getHost(),
            "couchbase.port", Integer.toString(container.getBootstrapHttpDirectPort()),
            "couchbase.username", container.getUsername(),
            "couchbase.password", container.getPassword(),
            "couchbase.bucket", BUCKET
        );
    }

    @Test
    void should_consume_bucket() {
        Bucket bucket = cluster.bucket(BUCKET);
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
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addRoutesBuilder(MyRouteBuilder.class);
    }
}
