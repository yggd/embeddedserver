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
package org.yggd.client.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Properties;

public class SftpClientBuilder {

    private final String host;
    private final int port;
    private final JSch jSch = new JSch();

    private String username;

    private Properties properties = new Properties();

    private File privateKey;

    private SftpClient client;

    public SftpClientBuilder(String host, int port) {
        this.host = host;
        this.port = port;
        properties.put("StrictHostKeyChecking", "no");
    }

    public SftpClientBuilder username(String username) {
        this.username = username;
        return this;
    }

    public SftpClientBuilder property(Object key, Object value) {
        this.properties.put(key, value);
        return this;
    }

    public SftpClientBuilder privateKey(Path path) {
        return privateKey(path.toFile().getAbsolutePath());
    }

    public SftpClientBuilder privateKey(String privateKey) {
        try {
            jSch.addIdentity(resourceToFile(privateKey).getAbsolutePath());
        } catch (JSchException e) {
            throw new IllegalArgumentException(e);
        }
        return this;
    }

    public SftpClientBuilder privateKey(Resource resource, String passphrase) {
        try {
            return privateKey(resource.getFile(), passphrase);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public SftpClientBuilder privateKey(File file, String passphrase) {
        return privateKey(file.getAbsolutePath(), passphrase);
    }

    public SftpClientBuilder privateKey(String privateKey, String passphrase) {
        try {
            jSch.addIdentity(privateKey, passphrase.getBytes());
        } catch (JSchException e) {
            throw new IllegalArgumentException(e);
        }
        return this;
    }

    public SftpClientImpl build() {
        final Session session = createSession();
        assert session != null;
        return new SftpClientImpl(session, createChannel(session));
    }

    private Session createSession() {
        try {
            Session session = jSch.getSession(username, host, port);
            session.setConfig(properties);
            session.connect();
            return session;
        } catch (JSchException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private ChannelSftp createChannel(Session session) {
        try {
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            return channelSftp;
        } catch (JSchException e) {
            throw new IllegalStateException(e);
        }
    }

    private File resourceToFile(String resource) {
        try {
            return new File(SftpClientBuilder.class.getResource(resource).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
