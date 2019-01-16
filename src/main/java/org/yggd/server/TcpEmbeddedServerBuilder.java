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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TcpEmbeddedServerBuilder implements EmbeddedServerBuilder {

    private int port;
    private Supplier<byte[]> active;
    private Consumer<byte[]> read;
    private boolean closeFromServer = false;

    TcpEmbeddedServerBuilder() {}

    public TcpEmbeddedServerBuilder port(int port) {
        this.port = port;
        return this;
    }

    public TcpEmbeddedServerBuilder active(Supplier<byte[]> active) {
        this.active = active;
        return this;
    }

    public TcpEmbeddedServerBuilder read(Consumer<byte[]> read) {
        this.read = read;
        return this;
    }

    public TcpEmbeddedServerBuilder closeFromServer(boolean closeFromServer) {
        this.closeFromServer = closeFromServer;
        return this;
    }

    @Override
    public EmbeddedServer build() {
        TcpServerImpl tcpServer = new TcpServerImpl(port);
        tcpServer.active(active);
        tcpServer.read(read);
        tcpServer.setCloseFromServer(closeFromServer);
        return tcpServer;
    }

    public static class TcpServerImpl implements EmbeddedServer {

        private final int port;
        private volatile boolean isRunning = false;
        private final EventLoopGroup group = new NioEventLoopGroup();
        private Supplier<byte[]> activeCallback;
        private Consumer<byte[]> readCallback;
        private boolean closeFromServer;

        private TcpServerImpl(int port) {
            this.port = port;
        }

        private void active(Supplier<byte[]> active) {
            this.activeCallback = active;
        }

        private void read(Consumer<byte[]> read) {
            this.readCallback = read;
        }

        private void setCloseFromServer(boolean closeFromServer) {
            this.closeFromServer = closeFromServer;
        }

        @Override
        public void start() {
            final ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(group)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new TcpServerHandler(activeCallback, readCallback, closeFromServer));
                        }
                    });
            try {
                bootstrap.bind().sync();
            } catch (InterruptedException e) {
                // through interrupt.
            }
            isRunning = true;
        }

        @Override
        public void stop() {
            try {
                isRunning = false;
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public boolean isRunning() {
            return isRunning;
        }
    }

    private static class TcpServerHandler extends ChannelInboundHandlerAdapter {

        private static final Logger logger = LoggerFactory.getLogger(TcpServerHandler.class);

        private final Supplier<byte[]> active;
        private final Consumer<byte[]> read;
        private final boolean closeFromServer;

        private TcpServerHandler(Supplier<byte[]> active, Consumer<byte[]> read, boolean closeFromServer) {
            this.active = active;
            this.read = read;
            this.closeFromServer = closeFromServer;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ctx.writeAndFlush(Unpooled.copiedBuffer(active.get()));
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf buf = (ByteBuf) msg;
            byte[] array;
            if (buf.hasArray()) {
                array = buf.array();
            } else {
                array = new byte[buf.readableBytes()];
                buf.getBytes(buf.readerIndex(), array);
            }
            read.accept(array);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            if (closeFromServer) {
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("exception occurs.", cause);
            ctx.close();
        }
    }
}
