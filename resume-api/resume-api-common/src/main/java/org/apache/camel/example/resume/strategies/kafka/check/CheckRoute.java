package org.apache.camel.example.resume.strategies.kafka.check;

import org.apache.camel.builder.RouteBuilder;

public class CheckRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("kafka:{{resume.type.kafka.topic}}?brokers={{bootstrap.address}}&maxBlockMs=5000&pollTimeoutMs=1000")
                .to("file:{{output.dir}}?fileName=summary.txt&fileExist=Append&appendChars=\n");
    }


}
