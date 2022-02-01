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
 * A basic integration test checking that Camel Servlet can be used with Apache Tomcat.
 */
@ExtendWith(ArquillianExtension.class)
public class ServletTomcatIT {

    @Deployment
    public static WebArchive createDeployment() {
        // build the .war with all the resources and libraries
        return ShrinkWrap.create(WebArchive.class, "camel-example-servlet-tomcat.war")
            .setWebXML(new File("./src/main/webapp/WEB-INF/web.xml"))
            .addAsResource("log4j2.properties")
            .addAsResource("camel-config.xml")
            .addAsLibraries(
                Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve()
                    .withTransitivity().asFile()
            );
    }

    @ArquillianResource
    URL url;

    @Test
    @RunAsClient
    void should_answer_without_the_name_parameter() {
        given()
            .baseUri(url.toString())
        .when()
            .get("/camel/hello")
        .then()
            .body(containsString("Add a name parameter to uri"));
    }

    @Test
    @RunAsClient
    void should_answer_with_the_name_parameter() {
        given()
            .baseUri(url.toString())
        .when()
            .queryParam("name", "foo")
            .get("/camel/hello")
        .then()
            .body(containsString("Hello foo how are you today?"));
    }
}
