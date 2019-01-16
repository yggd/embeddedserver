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
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.jcraft.jsch.ChannelSftp.LsEntrySelector.CONTINUE;

public class SftpClientImpl implements SftpClient, AutoCloseable {

    private final Session session;
    private ChannelSftp channelSftp;

    SftpClientImpl(Session session, ChannelSftp channelSftp) {
        this.session = session;
        this.channelSftp = channelSftp;
    }

    @Override
    public SftpClient connect() {
        try {
            session.connect();
            channelSftp = (ChannelSftp) session.openChannel("sftp");
        } catch (JSchException e) {
            throw new IllegalStateException(e);
        }
        return this;
    }

    @Override
    public void ls(String path, Consumer<ChannelSftp.LsEntry> c) {
        try {
            channelSftp.ls(path).forEach( e -> c.accept((ChannelSftp.LsEntry) e));
        } catch (SftpException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<ChannelSftp.LsEntry> lsMatch(String path,final Predicate<ChannelSftp.LsEntry> p) {
        final List<ChannelSftp.LsEntry> list = new ArrayList<>();
        try {
            channelSftp.ls(path, entry -> {
                if (p.test(entry)) {
                    list.add(entry);
                }
                return CONTINUE;
            });
        } catch (SftpException e) {
            throw new IllegalStateException(e);
        }
        return list;
    }

    @Override
    public void put(File f) {
        put(f.getAbsolutePath());
    }

    @Override
    public void put(String fileName) {
        try {
            channelSftp.put(fileName);
        } catch (SftpException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void put(String fileName, InputStream inputStream) {
        try {
            channelSftp.put(inputStream, fileName);
        } catch (SftpException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public InputStream get(String fileName) {
        try {
            return channelSftp.get(fileName);
        } catch (SftpException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void get(String fileName, OutputStream outputStream) {
        try {
            channelSftp.get(fileName, outputStream);
        } catch (SftpException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void disconnect() {
        close();
    }

    @Override
    public void close() {
        synchronized (this) {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }
}
