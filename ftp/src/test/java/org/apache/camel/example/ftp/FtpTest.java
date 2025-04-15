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
package org.apache.camel.example.ftp;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.main.MainConfigurationProperties;
import org.apache.camel.test.junit5.CamelContextConfiguration;
import org.apache.camel.test.main.junit5.CamelMainTestSupport;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.PropertiesUserManager;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.apache.camel.test.junit5.TestSupport.createDirectory;
import static org.apache.camel.test.junit5.TestSupport.deleteDirectory;
import static org.apache.camel.util.PropertiesHelper.asProperties;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A unit test checking that Camel can read/write from/to a ftp server.
 */
class FtpTest extends CamelMainTestSupport {

    private static FtpServer SERVER;
    private static int PORT;

    @BeforeAll
    static void init() throws Exception {
        ListenerFactory factory = new ListenerFactory();
        PORT = AvailablePortFinder.getNextAvailable();
        // set the port of the listener
        factory.setPort(PORT);
        FtpServerFactory serverFactory = new FtpServerFactory();
        // replace the default listener
        serverFactory.addListener("default", factory.createListener());

        // setup user management to read our users.properties and use clear text passwords
        File file = new File("src/test/resources/users.properties");
        UserManager userManager = new PropertiesUserManager(new ClearTextPasswordEncryptor(), file, "admin");
        serverFactory.setUserManager(userManager);

        NativeFileSystemFactory fsf = new NativeFileSystemFactory();
        serverFactory.setFileSystem(fsf);

        // Create the admin home
        createDirectory("./target/ftp-server");

        SERVER = serverFactory.createServer();
        // start the server
        SERVER.start();
    }

    @AfterAll
    static void destroy() {
        // Delete directories
        deleteDirectory("./target/ftp-server");
        deleteDirectory("./target/upload");
        if (SERVER != null) {
            SERVER.stop();
        }
    }

    @Override
    public void configureContext(CamelContextConfiguration camelContextConfiguration) {
        super.configureContext(camelContextConfiguration);
        Properties overridenProperties = asProperties(
                "ftp.port", Integer.toString(PORT),
                "ftp.username", "admin",
                "ftp.password", "admin");
        camelContextConfiguration.withUseOverridePropertiesWithPropertiesComponent(overridenProperties);
    }

    @Test
    void should_download_uploaded_file() throws IOException {
        String fileName = UUID.randomUUID().toString();
        assertTrue(
            new File(String.format("target/upload/%s", fileName)).createNewFile(),
            "The test file should be created"
        );
        NotifyBuilder notify = new NotifyBuilder(context)
            .whenCompleted(1).wereSentTo("ftp:*")
            .and().whenCompleted(1).wereSentTo("file:*").create();

        assertTrue(
            notify.matches(30, TimeUnit.SECONDS), "1 file should be transferred with success"
        );
        assertTrue(
            new File(String.format("target/download/%s", fileName)).exists(),
            "The test file should be uploaded"
        );
    }

    @Override
    protected void configure(MainConfigurationProperties configuration) {
        configuration.addRoutesBuilder(MyFtpClientRouteBuilder.class, MyFtpServerRouteBuilder.class);
    }
}
