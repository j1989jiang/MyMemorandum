package com.git.j1989jiang.base;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateClientExtensionHandshaker;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import static io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateServerExtensionHandshaker.MAX_WINDOW_SIZE;

public class NettyWebSocketBootStrap {

    private static NettyWebSocketBootStrap inst;

    Bootstrap bootstrap;
    URI webSocketURI;

    private NettyWebSocketBootStrap(String webSocketUrl, boolean useExtension) throws Exception {

        try {
            webSocketURI = new URI(webSocketUrl);
        } catch (URISyntaxException e) {
            e.printStackTrace();

        }


        final boolean needSsl = webSocketUrl.startsWith("wss");
        final String host = webSocketURI.getHost() == null ? "127.0.0.1" : webSocketURI.getHost();
        final int port;

        if (webSocketURI.getPort() == -1) {
            if (needSsl) {
                port = 443;
            } else {
                port = 80;
            }
        } else {
            port = webSocketURI.getPort();
        }

        final SslContext sslCtx;
        if (needSsl) {
            sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        EventLoopGroup group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();

        bootstrap.remoteAddress(InetSocketAddress.createUnresolved(webSocketURI.getHost(), port));
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000 * 10)
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline p = socketChannel.pipeline();
                        if (sslCtx != null) {
                            // 启动SSL加密链接，最外层
                            p.addLast(sslCtx.newHandler(socketChannel.alloc(), host, port));
                        }
                        // HTTP请求包装
                        p.addLast(new HttpClientCodec());
                        // POST请求body包装
                        p.addLast("aggregator", new HttpObjectAggregator(655360));
                        if (useExtension) {
                            // 启动数据压缩
                            p.addLast("exe", new WebSocketClientExtensionHandler(new PerMessageDeflateClientExtensionHandshaker(6, ZlibCodecFactory.isSupportingWindowSizeAndMemLevel(), MAX_WINDOW_SIZE, true, true)));
                        }
                        p.addLast("hookedHandler", new NettyWebSocketClientHandler());
                    }
                });

    }

    public Channel getChannel() {
        try {
            return bootstrap.connect().sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static NettyWebSocketBootStrap getInst(String webSocketUrl, boolean useExtension) {
        if (inst == null) {
            try {
                inst = new NettyWebSocketBootStrap(webSocketUrl, useExtension);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return inst;
    }


}
