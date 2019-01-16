/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.yggd.server;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.yggd.client.sftp.SftpClientBuilder;
import org.yggd.client.sftp.SftpClientImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class SftpEmbeddedServerBuilderTest {

    private static final int PORT = 10022;
    private static final Path TMP_PATH = Paths.get(System.getProperty("java.io.tmpdir"));
    private static Path home;
    private static String USER = "user1";
    private static EmbeddedServer sftpServer;


    @BeforeClass
    public static void setUpClass() throws IOException {
        home = Files.createTempDirectory(TMP_PATH, "sftpHome");
        sftpServer = ServerBuilder.withSftp()
                .port(PORT)
                .directory(home)
                .keyPairProvider(new ClassPathResource("/security/hostkey.ser"))
                .publicKeyAuthenticate(new ClassPathResource("/security/id_rsa.pub"),
                        (username, clientkey, session, serverkey) ->
                                USER.equals(username) && serverkey.equals(clientkey))
                .build();
        sftpServer.start();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        sftpServer.stop();
        Arrays.stream(home.toFile().listFiles()).forEach(f -> {
            try {
                Files.delete(f.toPath());
            } catch (IOException e) {
                // ignore.
            }
        });
        Files.delete(home);
    }

    @Test
    public void testSftpPut() {
        try (final SftpClientImpl client = new SftpClientBuilder("localhost", PORT)
                .username("user1")
                .privateKey(new ClassPathResource("security/id_rsa"), "password")
                .build()) {
            client.ls(".", System.out::println);
            client.put(".gitignore");
        }
    }
}