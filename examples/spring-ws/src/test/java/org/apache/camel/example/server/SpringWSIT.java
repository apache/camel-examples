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
package org.apache.camel.example.server;

import io.restassured.RestAssured;
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
import static io.restassured.config.XmlConfig.xmlConfig;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * A basic integration test checking that Camel can leverage Spring Web Services to expose a Web Service.
 */
@ExtendWith(ArquillianExtension.class)
public class SpringWSIT {

    @Deployment
    public static WebArchive createDeployment() {
        // build the .war with all the resources and libraries
        return ShrinkWrap.create(WebArchive.class, "camel-example-spring-ws.war")
            .setWebXML(new File("./src/main/webapp/WEB-INF/web.xml"))
            .addAsWebInfResource(new File("./src/main/webapp/WEB-INF/increment.xsd"))
            .addAsWebInfResource(new File("./src/main/webapp/WEB-INF/spring-ws-servlet.xml"))
            .addAsResource("log4j2.properties")
            .addAsResource("org/apache/camel/example/server/model/jaxb.index")
            .addAsLibraries(
                Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve()
                    .withTransitivity().asFile()
            );
    }

    @ArquillianResource
    URL url;

    @Test
    @RunAsClient
    void should_call_the_web_service() {
        given()
            .config(
                RestAssured.config().xmlConfig(
                    xmlConfig().declareNamespace("soapenv", "http://schemas.xmlsoap.org/soap/envelope/")
                        .declareNamespace("inc", "http://camel.apache.org/example/increment")
                )
            )
            .baseUri(url.toString())
        .when()
            .body("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:inc=\"http://camel.apache.org/example/increment\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <inc:incrementRequest>\n" +
                    "         <inc:input>90</inc:input>\n" +
                    "      </inc:incrementRequest>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>")
            .contentType("text/xml")
            .post("/increment")
        .then()
            .statusCode(200)
            .body("soapenv:Envelope.soapenv:Body.inc:incrementResponse.inc:result.text()", equalTo("91"));

    }

    @Test
    @RunAsClient
    void should_access_to_the_wsdl() {
        given()
            .baseUri(url.toString())
        .when()
            .get("/increment/increment.wsdl")
        .then()
            .statusCode(200)
            .contentType(is("text/xml"))
            .body(containsString("http://localhost"));
    }

}
