package org.apache.camel.ssh.examples;

import java.nio.file.Paths;
import java.security.PublicKey;

import org.apache.camel.main.Main;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ProcessShellFactory;

public class MainApp {

    public static void main(String... args) throws Exception {
        String mainArgumentExpected = getArgumentOrShowSyntax(args);

        SshServer sshServer = setupSshServer();
        sshServer.start();

        Main main = new Main();
        switch (mainArgumentExpected) {
            case "consumer":
                configureDatePollerRoute(main, sshServer);
                break;
            case "producer":
                configureFileProducerRoute(main, sshServer);
                break;
            default:
                throw new IllegalArgumentException("You must provide either consumer or producer argument!");
        }
        main.run();
    }

    private static String getArgumentOrShowSyntax(String[] args) {
        if (args.length == 0){
            System.out.println("Please provide either consumer or producer argument to run this application!");
            System.exit(-1);
        }
        return args[0];
    }

    private static void configureFileProducerRoute(Main main, SshServer sshServer) {
        main.configure().addRoutesBuilder(new FileProducerRouteBuilder(
                sshServer.getHost() == null ? "localhost" : sshServer.getHost(), sshServer.getPort())
        );
    }

    private static void configureDatePollerRoute(Main main, SshServer sshServer) {
        main.configure().addRoutesBuilder(new DatePollerRouteBuilder(
                sshServer.getHost() == null ? "localhost" : sshServer.getHost(), sshServer.getPort())
        );
    }

    private static SshServer setupSshServer() {
        int port = AvailablePortFinder.getNextAvailable();
        SshServer sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(port);

        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get("target/generatedkey.pem")));
        sshServer.setCommandFactory(command -> new ProcessShellFactory(command.split(";")).create());
        sshServer.setPasswordAuthenticator((user, password, session) -> validateUser(user, password));
        sshServer.setPublickeyAuthenticator((user, publicKey, session) -> validatePublicKey(user, publicKey));
        return sshServer;
    }

    private static boolean validatePublicKey(String user, PublicKey publicKey) {
        return "test".equals(user);
    }

    private static boolean validateUser(String user, String password) {
        return "test".equals(user) && "p455w0rd".equals(password);
    }

}

