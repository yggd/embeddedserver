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

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.UserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class FtpEmbeddedServerBuilder implements EmbeddedServerBuilder {

    private final List<User> users = new ArrayList<>();

    private final FtpServerFactory ftpServerFactory;
    private final ListenerFactory listenerFactory;
    private final UserManagerFactory userManagerFactory;

    private final List<Authority> authorities = new ArrayList<>();

    FtpEmbeddedServerBuilder(FtpServerFactory ftpServerFactory, ListenerFactory listenerFactory,
                                    UserManagerFactory userManagerFactory) {
        this.ftpServerFactory = ftpServerFactory;
        this.listenerFactory = listenerFactory;
        this.userManagerFactory = userManagerFactory;
    }

    @Override
    public FtpEmbeddedServer build() {
        ftpServerFactory.addListener("default", listenerFactory.createListener());
        final UserManager userManager = userManagerFactory.createUserManager();
        users.forEach(u -> {
            try {
                userManager.save(u);
            } catch (FtpException e) {
                throw new EmbeddedServerException(e);
            }
        });
        ftpServerFactory.setUserManager(userManager);
        return new FtpEmbeddedServer(ftpServerFactory.createServer());
    }


    public FtpEmbeddedServerBuilder filesystem(Consumer<NativeFileSystemFactory> c) {
        NativeFileSystemFactory fileSystemFactory = new NativeFileSystemFactory();
        c.accept(fileSystemFactory);
        ftpServerFactory.setFileSystem(fileSystemFactory);
        return this;
    }

    public FtpEmbeddedServerBuilder port(int port) {
        listenerFactory.setPort(port);
        return this;
    }

    public FtpEmbeddedServerBuilder ssl(Consumer<SslConfig> sslConfigConsumer) {
        final SslConfig sslConfig = new SslConfig(listenerFactory);
        sslConfigConsumer.accept(sslConfig);
        listenerFactory.setSslConfiguration(sslConfig.getConfiguration());
        return this;
    }

    public FtpEmbeddedServerBuilder user(Consumer<BaseUser> consumer) {
        return user(consumer, false);
    }

    public FtpEmbeddedServerBuilder user(Consumer<BaseUser> consumer, boolean readOnly) {
        final BaseUser baseUser = new BaseUser();
        if (!readOnly) {
            baseUser.setAuthorities(Collections.singletonList(new WritePermission()));
        }
        consumer.accept(baseUser);
        users.add(baseUser);
        return this;
    }

    private static class FtpEmbeddedServer implements EmbeddedServer {

        private final FtpServer ftpServer;

        private FtpEmbeddedServer(FtpServer ftpServer) {
            this.ftpServer = ftpServer;
        }

        @Override
        public void start() {
            try {
                ftpServer.start();
            } catch (FtpException e) {
                throw new EmbeddedServerException(e);
            }
        }

        @Override
        public void stop() {
            ftpServer.stop();
        }

        @Override
        public boolean isRunning() {
            return !ftpServer.isStopped() && !ftpServer.isSuspended();
        }
    }

    public static class SslConfig {

        private final SslConfigurationFactory sslConfigurationFactory = new SslConfigurationFactory();
        private final ListenerFactory listenerFactory;

        private SslConfig(ListenerFactory listenerFactory) {
            this.listenerFactory = listenerFactory;
        }

        public void keystoreString(String keystore) {
            sslConfigurationFactory.setKeystoreFile(new File(keystore));
        }

        public void keystorePath(Path keystore) {
            sslConfigurationFactory.setKeystoreFile(keystore.toFile());
        }

        public void implicit() {
            listenerFactory.setImplicitSsl(true);
        }

        public void explicit() {
            listenerFactory.setImplicitSsl(false);
        }

        public void keystoreResource(File keystore) {
            sslConfigurationFactory.setKeystoreFile(keystore);
        }

        public void keyAlias(String keyAlias) {
            sslConfigurationFactory.setKeyAlias(keyAlias);
        }

        public void keystorePassword(String keystorePassword) {
            sslConfigurationFactory.setKeystorePassword(keystorePassword);
        }

        public void keyPassword(String keyPassword) {
            sslConfigurationFactory.setKeyPassword(keyPassword);
        }

        public void keystoreAlgorithm(String keystoreAlgorithm) {
            sslConfigurationFactory.setKeystoreAlgorithm(keystoreAlgorithm);
        }

        public void truststoreFile(File truststoreFile) {
            sslConfigurationFactory.setTruststoreFile(truststoreFile);
        }

        public void truststoreType(String truststoreType) {
            sslConfigurationFactory.setTruststoreType(truststoreType);
        }

        public void truststorePassword(String truststorePassword) {
            sslConfigurationFactory.setTruststorePassword(truststorePassword);
        }

        public void truststoreAlgorithm(String truststoreAlgorithm) {
            sslConfigurationFactory.setTruststoreAlgorithm(truststoreAlgorithm);
        }

        public void sslProtocol(String sslProtocol) {
            sslConfigurationFactory.setSslProtocol(sslProtocol);
        }

        public void enabledCipherSuites(String[] enabledCipherSuites) {
            sslConfigurationFactory.setEnabledCipherSuites(enabledCipherSuites);
        }

        public void clientAuthentication(String clientAuthentication) {
            sslConfigurationFactory.setClientAuthentication(clientAuthentication);
        }

        private SslConfiguration getConfiguration() {
            return sslConfigurationFactory.createSslConfiguration();
        }
    }
}
