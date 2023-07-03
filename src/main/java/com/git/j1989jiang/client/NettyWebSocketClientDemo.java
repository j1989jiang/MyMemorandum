package com.git.j1989jiang.client;

import com.git.j1989jiang.base.AbstractNettyWebSocketClient;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;

public class NettyWebSocketClientDemo extends AbstractNettyWebSocketClient {

    public NettyWebSocketClientDemo(long clientId, String webSocketUrl, boolean useExtension) {
        super(clientId, webSocketUrl, useExtension);
    }

    @Override
    public void onOpen(HttpHeaders headers) {

    }

    @Override
    public void onMessage(String msg) {

    }

    @Override
    public void onMessage(ByteBuf buf) {

    }

    @Override
    public void onClose() {

    }
}
