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
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.manager.bucket.BucketType;
import com.couchbase.client.java.manager.view.DesignDocument;
import com.couchbase.client.java.manager.view.View;
import com.couchbase.client.java.view.DesignDocumentNamespace;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.couchbase.CouchbaseConstants;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.test.infra.couchbase.services.CouchbaseService;
import org.apache.camel.test.infra.couchbase.services.CouchbaseServiceFactory;
import org.apache.camel.test.main.junit5.CamelMainTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.apache.camel.util.PropertiesHelper.asProperties;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel consume data from Couchbase.
 */
class CouchbaseTest extends CamelMainTestSupport {

    private static final String BUCKET = "test-bucket-" + System.currentTimeMillis();

    @RegisterExtension
    private static final CouchbaseService SERVICE = CouchbaseServiceFactory.createService();
    private static Cluster CLUSTER;

    @BeforeAll
    static void init() {
        CLUSTER = Cluster.connect(
            SERVICE.getConnectionString(), SERVICE.getUsername(), SERVICE.getPassword()
        );
        DesignDocument designDoc = new DesignDocument(
            CouchbaseConstants.DEFAULT_DESIGN_DOCUMENT_NAME,
            Collections.singletonMap(
                CouchbaseConstants.DEFAULT_VIEWNAME,
                new View("function (doc, meta) {  emit(meta.id, doc);}")
            )
        );
        CLUSTER.buckets().createBucket(
                BucketSettings.create(BUCKET).bucketType(BucketType.COUCHBASE).flushEnabled(true));
        CLUSTER.bucket(BUCKET).viewIndexes().upsertDesignDocument(designDoc, DesignDocumentNamespace.PRODUCTION);
    }

    @AfterAll
    static void destroy() {
        if (CLUSTER != null) {
            CLUSTER.buckets().dropBucket(BUCKET);
            CLUSTER.disconnect();
        }
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        return asProperties(
            "couchbase.host", SERVICE.getHostname(),
            "couchbase.port", Integer.toString(SERVICE.getPort()),
            "couchbase.username", SERVICE.getUsername(),
            "couchbase.password", SERVICE.getPassword(),
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
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addRoutesBuilder(MyRouteBuilder.class);
    }
}
