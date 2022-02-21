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
package org.apache.camel.example.mongodb;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.client.MongoClients;
import io.restassured.response.Response;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.spi.Registry;
import org.apache.camel.test.main.junit5.CamelMainTestSupport;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel can execute CRUD operations against MongoDB.
 */
@Testcontainers
class MongoDBTest extends CamelMainTestSupport {

    private static final String IMAGE = "mongo:5.0";

    private static final String BASE_URI = "http://localhost:8081";

    @Container
    private final MongoDBContainer container = new MongoDBContainer(DockerImageName.parse(IMAGE));

    @Override
    protected void bindToRegistry(Registry registry) throws Exception {
        registry.bind(
            "myDb",
            MongoClients.create(String.format("mongodb://%s:%d", container.getHost(), container.getMappedPort(27017)))
        );
    }

    @Test
    void should_execute_crud_operations() throws Exception {
        // Insert a Document
        Response response = given()
            .baseUri(BASE_URI)
        .when()
            .contentType("application/json")
            .body("{\"text\":\"Hello from Camel\"}")
            .post("/hello")
        .then()
            .body(matchesPattern("Document\\{\\{text=Hello from Camel, _id=(.*)}}"))
        .extract()
            .response();
        Matcher matcher = Pattern.compile(".*_id=(.*)}}.*").matcher(response.asString());
        assertTrue(matcher.find(), "The response should match the regular expression");
        String id = matcher.group(1);
        // Find By Id
        given()
            .baseUri(BASE_URI)
        .when()
            .queryParam("id", id)
            .get("/hello")
        .then()
            .body(containsString(String.format("_id=%s", id)));
        // Find All
        given()
            .baseUri(BASE_URI)
        .when()
            .get("/")
        .then()
            .body(containsString(String.format("_id=%s", id)));
    }

    @Override
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addRoutesBuilder(new MongoDBFindByIDRouteBuilder());
        configuration.addRoutesBuilder(new MongoDBFindAllRouteBuilder());
        configuration.addRoutesBuilder(new MongoDBInsertRouteBuilder());
    }
}
