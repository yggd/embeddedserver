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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.yggd.client.tcp.TcpClient;
import org.yggd.client.tcp.TcpClientImpl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class TcpEmbeddedServerBuilderTest {

    private static EmbeddedServer tcpServer;
    private static final BlockingQueue<byte[]> serverRead = new ArrayBlockingQueue<>(10);
    private static final int PORT = 12345;

    @BeforeClass
    public static void setUpClass() {
        tcpServer = ServerBuilder.withTcp()
                .port(PORT)
                .active( () ->  "response".getBytes())
                .read( b -> {
                    try {
                        serverRead.put(b);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                })
                .build();

        tcpServer.start();
    }

    @Before
    public void setUp() {
        serverRead.clear();
    }

    @Test
    public void testTcpExchange() throws Exception {
        final TcpClient client = new TcpClientImpl().connect("localhost", PORT);

        final byte[] exchange = client.exchange("request".getBytes());

        assertThat(new String(serverRead.take()), is("request"));
        assertThat(new String(exchange), is("response"));
    }

    @AfterClass
    public static void tearDownClass() {
        if (tcpServer != null && tcpServer.isRunning()) {
            tcpServer.stop();
        }
    }
}