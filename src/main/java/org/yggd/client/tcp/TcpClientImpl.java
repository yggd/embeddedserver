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
package org.yggd.client.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class TcpClientImpl implements TcpClient {

    private final Bootstrap bootStrap = new Bootstrap();
    private final EventLoopGroup group = new NioEventLoopGroup();
    private int timeout = -1;

    @Override
    public TcpClient connect(String host, int port) {
        this.bootStrap.group(group)
                .channel(NioSocketChannel.class)
                .remoteAddress(new InetSocketAddress(host, port));
        return this;
    }

    @Override
    public TcpClient timeout(long timeout, TimeUnit timeUnit) {
        if (timeout > 0) {
            this.timeout = (int) timeUnit.toSeconds(timeout);
        }
        return this;
    }

    @Override
    public byte[] exchange(byte[] buf) {
        final InboundHandler inboundHandler = new InboundHandler(buf);
        bootStrap.handler(new ChannelInitializer<SocketChannel>(){
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                if (timeout > 0) {
                    ch.pipeline().addLast(new ReadTimeoutHandler(timeout));
                }
                ch.pipeline().addLast(inboundHandler);
            }
        });
        try {
            final ChannelFuture channelFuture = bootStrap.connect().sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            // do nothing.
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                // do nothing.
            }
        }
        return inboundHandler.response();
    }

    private static class InboundHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private static final Logger logger = LoggerFactory.getLogger(InboundHandler.class);
        private final ByteBuf request;
        private BlockingQueue<byte[]> responseQueue = new ArrayBlockingQueue<>(1);

        private InboundHandler(byte[] rawBytes) {
            this.request = Unpooled.copiedBuffer(rawBytes);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error("exception cause.", cause);
            ctx.close();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            responseQueue.clear();
            ctx.writeAndFlush(request);
        }

        private byte[] retrieveByteArray(ByteBuf buf) {
            if (buf.hasArray()) {
                return buf.array();
            }
            byte[] heapArray = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), heapArray);
            return heapArray;
        }

        byte[] response() {
            return responseQueue.poll();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
            responseQueue.put(retrieveByteArray(byteBuf));
            channelHandlerContext.close();
        }
    }
}
