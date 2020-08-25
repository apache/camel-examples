package org.apache.camel.ssh.examples;

import org.apache.camel.builder.RouteBuilder;

public class DatePollerRouteBuilder extends RouteBuilder {

    private static final String USER = "test";
    private static final String PASSWORD = "p455w0rd";
    private static final String POLL_COMMAND = "date";
    private static final int POLL_DELAY = 10000;

    String sshHost;
    int sshPort;

    public DatePollerRouteBuilder(String sshHost, int sshPort) {
        this.sshHost = sshHost;
        this.sshPort = sshPort;
    }

    public void configure() {
        from(String.format("ssh:%s:%s?username=%s&password=%s&pollCommand=%s&delay=%d",
                sshHost,
                sshPort,
                USER,
                PASSWORD,
                POLL_COMMAND,
                POLL_DELAY))
                .transform(bodyAs(String.class))
                .log("${body}");
    }

}
