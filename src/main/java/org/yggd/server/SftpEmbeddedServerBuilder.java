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

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class SftpEmbeddedServerBuilder implements EmbeddedServerBuilder {

    private final SshServer sshServer;

    SftpEmbeddedServerBuilder(SshServer sshServer) {
        this.sshServer = sshServer;
    }

    @FunctionalInterface
    public interface InnerAuthenticator {
        boolean authenticate(String username, PublicKey clientKey, ServerSession session, PublicKey serverKey);
    }

    public SftpEmbeddedServerBuilder port(int port) {
        sshServer.setPort(port);
        sshServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        return this;
    }

    public SftpEmbeddedServerBuilder directory(Path dir) {
        if (!dir.toFile().mkdirs() && !dir.toFile().exists()) {
            throw new EmbeddedServerException("failed create directory:" + dir);
        }
        sshServer.setFileSystemFactory(new VirtualFileSystemFactory(dir));
        return this;
    }

    public SftpEmbeddedServerBuilder keyPairProvider(Resource resource) {
        try {
            return keyPairProvider(resource.getURI().getPath());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public SftpEmbeddedServerBuilder keyPairProvider(Path path) {
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(path));
        return this;
    }

    public SftpEmbeddedServerBuilder keyPairProvider(String keypairProvider) {
        return keyPairProvider(Paths.get(keypairProvider));
    }

    public SftpEmbeddedServerBuilder passwordAuthenticate(PasswordAuthenticator authenticator) {
        sshServer.setPasswordAuthenticator(authenticator);
        return this;
    }

    public SftpEmbeddedServerBuilder publicKeyAuthenticate(Resource resource, InnerAuthenticator authenticator) {
        try {
            return publicKeyAuthenticate(resource.getFile(), authenticator);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public SftpEmbeddedServerBuilder publicKeyAuthenticate(Path path, InnerAuthenticator authenticator) {
        return publicKeyAuthenticate(path.toFile(), authenticator);
    }

    public SftpEmbeddedServerBuilder publicKeyAuthenticate(File publicKeyFile, InnerAuthenticator authenticator) {
        sshServer.setPublickeyAuthenticator((username, key, session) ->
                authenticator.authenticate(username, key,session, publicKey(publicKeyFile)));
        return this;
    }

    private PublicKey publicKey(File publicKeyFile) {
        try {
            final List<String> lines = Files.readAllLines(publicKeyFile.toPath());
            if (lines.size() != 1) {
                throw new EmbeddedServerException("publicKey must include single key entry.");
            }
            final String[] splitKeys = lines.iterator().next().split(" ");
            if (splitKeys.length < 2) {
                throw new EmbeddedServerException("invalid publicKey format, split more than 2 word by space.");
            }
            final String algorithm = splitKeys[0];
            if (!"ssh-rsa".equals(algorithm)) {
                throw new EmbeddedServerException("support only RSA publicKey type.");
            }
            final String base64Key = splitKeys[1];
            if (!base64Key.startsWith("AAAA")) {
                throw new EmbeddedServerException("invalid publicKey character array, must start with AAAA.");
            }
            final ByteBuffer keyBuf = ByteBuffer.wrap(Base64.getDecoder().decode(base64Key));
            final String decodedAlgorithm = decodeAlgorithm(keyBuf);
            if (!algorithm.equals(decodedAlgorithm)) {
                throw new EmbeddedServerException("algorithm mismatched between public key header(" + algorithm + ") and decoded raw key(" + decodedAlgorithm + ").");
            }
            final BigInteger exponent = decodeBigInt(keyBuf);
            final BigInteger modulus = decodeBigInt(keyBuf);
            return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new EmbeddedServerException(e);
        }
    }

    private String decodeAlgorithm(ByteBuffer bb) {
        final int len = decodeInt(bb);
        byte[] raw = new byte[len];
        bb.get(raw);
        return new String(raw, 0, len);
    }

    private int decodeInt(ByteBuffer bb) {
        return bb.getInt();
    }

    private BigInteger decodeBigInt(ByteBuffer bb) {
        int len = bb.getInt();
        byte[] bytes = new byte[len];
        bb.get(bytes);
        return new BigInteger(bytes);
    }

    @Override
    public EmbeddedServer build() {
        return new SshEmbeddedServer(sshServer);
    }

    private static class SshEmbeddedServer implements EmbeddedServer {

        private final SshServer sshServer;

        private SshEmbeddedServer(SshServer sshServer) {
            this.sshServer = sshServer;
        }

        @Override
        public void start() {
            try {
                sshServer.start();
            } catch (IOException e) {
                throw new EmbeddedServerException(e);
            }
        }

        @Override
        public void stop() {
            try {
                sshServer.stop();
            } catch (IOException e) {
                throw new EmbeddedServerException(e);
            }
        }

        @Override
        public boolean isRunning() {
            return sshServer.isStarted();
        }
    }
}
