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
package org.apache.camel.example.cxf;

import org.apache.camel.example.cxf.incident.*;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

import static org.apache.camel.example.cxf.CamelRouteClient.createCXFClient;
import static org.apache.camel.example.cxf.CamelRouteClient.createInputReportIncident;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A basic integration test checking that a web service can be exposed from Apache Tomcat thanks to Camel CXF code first.
 */
@ExtendWith(ArquillianExtension.class)
public class CXFTomcatIT {

    @Deployment
    public static WebArchive createDeployment() {
        // build the .war with all the resources, classes and libraries
        return ShrinkWrap.create(WebArchive.class, "camel-example-cxf-tomcat.war")
            .setWebXML(new File("./src/main/webapp/WEB-INF/web.xml"))
            .addPackages(true, CXFTomcatIT.class.getPackage())
            .addAsResource("log4j2.properties")
            .addAsResource("camel-config.xml")
            .addAsLibraries(
                Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve()
                    .withTransitivity().asFile()
            );
    }

    @Test
    @RunAsClient
    void should_communicate_with_the_web_service() {
        // create input parameter
        InputReportIncident input = createInputReportIncident();

        // create the webservice client and send the request
        IncidentService client = createCXFClient();
        OutputReportIncident out = client.reportIncident(input);
        assertEquals("OK;123", out.getCode());
        InputStatusIncident inStatus = new InputStatusIncident();
        inStatus.setIncidentId("456");
        OutputStatusIncident outStatus = client.statusIncident(inStatus);
        assertEquals("IN PROGRESS", outStatus.getStatus());
    }
}
