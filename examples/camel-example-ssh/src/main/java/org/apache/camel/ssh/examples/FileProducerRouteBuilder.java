package org.apache.camel.ssh.examples;

import org.apache.camel.builder.RouteBuilder;

public class FileProducerRouteBuilder extends RouteBuilder {

    private static final String USER = "test";
    private static final String PASSWORD = "p455w0rd";

    String sshHost;
    int sshPort;

    public FileProducerRouteBuilder(String sshHost, int sshPort) {
        this.sshHost = sshHost;
        this.sshPort = sshPort;
    }

    public void configure() {
        from("timer:timer?period=10000")
                .setBody(constant("sh;ssh/command.sh"))
                .to(String.format("ssh:%s:%s?username=%s&password=%s",
                        sshHost,
                        sshPort,
                        USER,
                        PASSWORD))
                .log("${body}");
    }

}
