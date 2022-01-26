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
package org.apache.camel.example.oaipmh;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.builder.Namespaces;

public class OAIPMHRouteBuilder extends RouteBuilder {

    @Override
    public void configure() {

        from("oaipmh://export.arxiv.org/oai2?"
                + "verb=ListSets"
                + "&onlyFirst=true"
                + "&delay=1000"
                + "&initialDelay=1000")
                .split(xpath("/default:OAI-PMH/default:ListSets/default:set",
                        new Namespaces("default", "http://www.openarchives.org/OAI/2.0/")))
                .filter().xpath("default:set/default:setName='Mathematics'",
                        new Namespaces("default", "http://www.openarchives.org/OAI/2.0/"))
                .to ("direct:ListRecords");
                
        from("direct:ListRecords")
                .setHeader("CamelOaimphSet", xpath("/default:set/default:setSpec/text()",
                         new Namespaces("default", "http://www.openarchives.org/OAI/2.0/")))
                //Prevent error message by request overload
                .delay(10000)
                .setHeader("CamelOaimphOnlyFirst", constant("true"))        
                .to("oaipmh://export.arxiv.org/oai2?")
                        .split(body())
                        .split(xpath("/default:OAI-PMH/default:ListRecords/default:record/default:metadata/oai_dc:dc/dc:title/text()",
                                new Namespaces("default", "http://www.openarchives.org/OAI/2.0/")
                                        .add("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/")
                                        .add("dc", "http://purl.org/dc/elements/1.1/")))
                //Log the titles of the records
                        .to("log:titles")
                        .to("mock:result");
    }

}
