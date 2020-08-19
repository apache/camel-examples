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
package org.apache.camel.example.cdi.minio;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

import io.minio.MinioClient;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.processor.idempotent.FileIdempotentRepository;

/**
 * Configures all our Camel routes, components, endpoints and beans
 */
public class Application extends RouteBuilder {
    final Properties properties = MinioExampleUtils.loadMinioPropertiesFile();

    public Application() throws IOException {
    }

    @Produces
    @Named("minioClient")
    MinioClient minioClient() {
        MinioClient.Builder client = MinioClient.builder()
                .endpoint(properties.getProperty("endpoint"))
                .credentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"))
                .region(properties.getProperty("region"));
        return client.build();
    }

    @Override
    public void configure() {
        from("minio://bucket-name?deleteAfterRead=false&maxMessagesPerPoll=25&delay=5000")
                .log(LoggingLevel.INFO, "consuming", "Consumer Fired!")
                .idempotentConsumer(header("CamelMinioETag"),
                        FileIdempotentRepository.fileIdempotentRepository(new File("target/file.data"), 250, 512000))
                .log(LoggingLevel.INFO, "Replay Message Sent to file:minio_out ${in.header.CamelMinioObjectName}")
                .to("file:target/minio_out?fileName=${in.header.CamelMinioObjectName}");
    }

}
