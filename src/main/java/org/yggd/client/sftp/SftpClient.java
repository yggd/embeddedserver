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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface SftpClient {

    SftpClient connect();
    void ls(String path, Consumer<ChannelSftp.LsEntry> c);
    List<ChannelSftp.LsEntry> lsMatch(String path, Predicate<ChannelSftp.LsEntry> p);

    void put(File f);
    void put(String fileName);
    void put(String fileName, InputStream inputStream);

    InputStream get(String fileName);
    void get(String fileName, OutputStream outputStream);

    void disconnect();
}
