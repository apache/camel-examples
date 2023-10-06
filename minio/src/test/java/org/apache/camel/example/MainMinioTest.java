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

import io.minio.MinioClient;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.minio.MinioComponent;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.test.infra.minio.services.MinioService;
import org.apache.camel.test.infra.minio.services.MinioServiceFactory;
import org.apache.camel.test.main.junit5.CamelMainTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MainMinioTest extends CamelMainTestSupport {

    @RegisterExtension
    private static final MinioService MINIO_SERVICE = MinioServiceFactory.createService();


    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();
        MinioComponent minio = camelContext.getComponent("minio", MinioComponent.class);

        minio.getConfiguration().setMinioClient(MinioClient.builder()
                .endpoint("http://" + MINIO_SERVICE.host(), MINIO_SERVICE.port(), false)
                .credentials(MINIO_SERVICE.accessKey(), MINIO_SERVICE.secretKey())
                .build());
        return camelContext;
    }

    @Test
    void should_consume_and_poll_minio_bucket() {

        NotifyBuilder notify = new NotifyBuilder(context).fromRoute("minio-consumer").whenCompleted(1).create();
        assertTrue(
                notify.matches(20, TimeUnit.SECONDS), "1 message should be completed"
        );

    }

    @Test
    void should_produce_and_put_in_minio_bucket() {

        NotifyBuilder notify = new NotifyBuilder(context).fromRoute("minio-producer").whenCompleted(1).create();
        assertTrue(
                notify.matches(20, TimeUnit.SECONDS), "1 file should be created"
        );

    }

    @Override
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addRoutesBuilder(MinioConsumer.class);
        configuration.addRoutesBuilder(MinioProducer.class);
    }

}
