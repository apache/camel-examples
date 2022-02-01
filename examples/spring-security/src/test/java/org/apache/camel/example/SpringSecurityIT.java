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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.net.URL;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * A basic integration test checking that Camel can leverage Spring Security to secure the endpoints.
 */
@ExtendWith(ArquillianExtension.class)
public class SpringSecurityIT {

    @Deployment
    public static WebArchive createDeployment() {
        // build the .war with all the resources and libraries
        return ShrinkWrap.create(WebArchive.class, "camel-example-spring-security.war")
            .setWebXML(new File("./src/main/webapp/WEB-INF/web.xml"))
            .addAsResource("log4j2.properties")
            .addAsResource("camel-context.xml")
            .addAsLibraries(
                Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve()
                    .withTransitivity().asFile()
            );
    }

    @ArquillianResource
    URL url;

    @Test
    @RunAsClient
    void should_prevent_anonymous_access_to_admin() {
        given()
            .baseUri(url.toString())
        .when()
            .get("/camel/admin")
        .then()
            .statusCode(401);
    }

    @Test
    @RunAsClient
    void should_prevent_bob_to_access_to_admin() {
        given()
            .baseUri(url.toString())
            .auth().basic("bob", "bobspassword")
        .when()
            .get("/camel/admin")
        .then()
            .body(containsString("Access Denied with the Policy"));
    }

    @Test
    @RunAsClient
    void should_allow_jim_to_access_to_admin() {
        given()
            .baseUri(url.toString())
            .auth().basic("jim", "jimspassword")
        .when()
            .get("/camel/admin")
        .then()
            .statusCode(200)
            .body(containsString("OK"));
    }

    @Test
    @RunAsClient
    void should_prevent_anonymous_access_to_user() {
        given()
            .baseUri(url.toString())
        .when()
            .get("/camel/user")
        .then()
            .statusCode(401);
    }

    @Test
    @RunAsClient
    void should_allow_bob_to_access_to_user() {
        given()
            .baseUri(url.toString())
            .auth().basic("bob", "bobspassword")
        .when()
            .get("/camel/user")
        .then()
            .statusCode(200)
            .body(containsString("Normal user"));
    }

    @Test
    @RunAsClient
    void should_allow_jim_to_access_to_user() {
        given()
            .baseUri(url.toString())
            .auth().basic("jim", "jimspassword")
        .when()
            .get("/camel/user")
        .then()
            .statusCode(200)
            .body(containsString("Normal user"));
    }

}
