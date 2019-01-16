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

import org.apache.commons.net.ftp.FTPClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.yggd.server.EmbeddedServer;
import org.yggd.server.ServerBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class FtpEmbeddedServerBuilderTest {

    private static final int PORT = 10021;
    private static final String USER = "user1";
    private static final String PASSWORD = "password";

    private static EmbeddedServer ftpServer;
    private static Path directory;

    @BeforeClass
    public static void setUpClass() throws IOException {
        directory = Files.createTempDirectory(Paths.get("/tmp"), "ftpTest");
        ftpServer = ServerBuilder.withFtp().port(PORT).user(u -> {
            u.setName("user1");
            u.setPassword("password");
            u.setHomeDirectory(directory.toString());
        }).filesystem(f -> f.setCreateHome(true)).build();
        ftpServer.start();
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        if (ftpServer != null) {
            ftpServer.stop();
        }
        if (directory != null && directory.toFile().exists()) {
            Arrays.stream(directory.toFile().listFiles()).forEach(f -> {
                try {
                    Files.deleteIfExists(f.toPath());
                } catch (IOException e) {
                    // ignore.
                }
            });
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void testFtpPut() throws Exception {
        final FTPClient client = new FTPClient();
        client.connect("localhost", PORT);
        client.login(USER, PASSWORD);
        final File tempFile = createTempFile(Paths.get("/tmp"));
        try (FileInputStream fis = new FileInputStream(tempFile)) {
            client.storeFile(tempFile.getName(), fis);
        }
        client.disconnect();

        // assert ftp-put file.
        final File putFile = new File(directory.toFile(), tempFile.getName());
        assertTrue(putFile.exists());
        assertThat(Files.readAllBytes(tempFile.toPath()), is(Files.readAllBytes(tempFile.toPath())));
        Files.delete(tempFile.toPath());
    }

    @Test
    public void testFtpGet() throws Exception {
        final File tempFile = createTempFile(directory);
        final FTPClient client = new FTPClient();
        client.connect("localhost", PORT);
        client.login(USER, PASSWORD);

        // assert ftp-get file.
        assertTrue(
            Arrays.stream(client.listFiles()).anyMatch(f ->
                f.getName().equals(tempFile.getName())
            )
        );
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        client.retrieveFile(tempFile.getName(), baos);
        assertThat(baos.toByteArray(), is(Files.readAllBytes(tempFile.toPath())));
    }

    private File createTempFile(final Path path) throws IOException {
        final File tempFile = Files.createTempFile(path, "ftpTest01", ".dat").toFile();
        assertTrue(tempFile.exists());
        try (FileWriter f = new FileWriter(tempFile)) {
            f.write(UUID.randomUUID().toString());
        }
        return tempFile;
    }
}