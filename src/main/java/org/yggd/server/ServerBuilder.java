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

import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.sshd.server.SshServer;

public class ServerBuilder {

    public static FtpEmbeddedServerBuilder withFtp() {
        return new FtpEmbeddedServerBuilder(new FtpServerFactory(), new ListenerFactory(), new PropertiesUserManagerFactory());
    }

    public static SftpEmbeddedServerBuilder withSftp() {
        return new SftpEmbeddedServerBuilder(SshServer.setUpDefaultServer());
    }

    public static TcpEmbeddedServerBuilder withTcp() {
        return new TcpEmbeddedServerBuilder();
    }
}
