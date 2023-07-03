package com.git.j1989jiang.base;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public abstract class AbstractNettyWebSocketClient {
    Logger logger = LoggerFactory.getLogger(AbstractNettyWebSocketClient.class);

    protected final long clientId;

    private final String webSocketUrl;

    private final boolean useExtension;

    private Channel channel;

    private boolean connected = false;

    private int maxMissPongCount;


    abstract public void onOpen(HttpHeaders headers);

    abstract public void onMessage(String msg);

    abstract public void onMessage(ByteBuf buf);

    abstract public void onClose();

    public AbstractNettyWebSocketClient(long clientId, String webSocketUrl, boolean useExtension) {
        this(clientId, webSocketUrl, useExtension, 10);
    }

    public AbstractNettyWebSocketClient(long clientId, String webSocketUrl, boolean useExtension, int maxMissPongCount) {
        this.clientId = clientId;
        this.webSocketUrl = webSocketUrl;
        this.useExtension = useExtension;
        this.maxMissPongCount = maxMissPongCount;
        NettyWebSocketClientManager.addClient(this);
    }


    public void connect() {
        channel = NettyWebSocketBootStrap.getInst(this.webSocketUrl, useExtension).getChannel();
        // 参数初始化
        channel.attr(NettyAttribute.CLIENT_ID).set(this.clientId);
        channel.attr(NettyAttribute.RECEIVED_PONG).set(true);
        channel.attr(NettyAttribute.PONG_MISS_COUNT).set(0);
        // 握手
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        URI webSocketURI = null;
        try {
            webSocketURI = new URI(webSocketUrl);
        } catch (URISyntaxException e) {
            logger.error("错误的Url:{}", webSocketUrl);
        }

        WebSocketClientHandshaker handShaker = WebSocketClientHandshakerFactory.newHandshaker(webSocketURI, WebSocketVersion.V13, null, true, httpHeaders, 655360);
        NettyWebSocketClientHandler handler = (NettyWebSocketClientHandler) channel.pipeline().get("hookedHandler");
        handler.setHandshaker(handShaker);
        handShaker.handshake(channel);
        //阻塞等待是否握手成功
        try {
            handler.handshakeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("连接失败：线程被中断");
        }
    }

    public void ping() {
        if (!channel.attr(NettyAttribute.RECEIVED_PONG).get()) {
            //上次的ping消息没有收到pong返回
            int pongMissCount = channel.attr(NettyAttribute.PONG_MISS_COUNT).get();
            pongMissCount++;
            if (pongMissCount >= maxMissPongCount) {
                logger.error("连接中断：心跳连续丢失超过{}次", maxMissPongCount);
                close();
            }
        }
        channel.writeAndFlush(new PingWebSocketFrame());
        channel.attr(NettyAttribute.RECEIVED_PONG).set(false);
    }

    public void pong() {
        channel.writeAndFlush(new PongWebSocketFrame());
    }

    public void onPing() {
        pong();
    }

    public void onPong() {
        channel.attr(NettyAttribute.RECEIVED_PONG).set(true);
        channel.attr(NettyAttribute.PONG_MISS_COUNT).set(0);
    }

    public void sendMessage(String msg) {
        TextWebSocketFrame textFrame = new TextWebSocketFrame(msg);
        channel.writeAndFlush(textFrame);
    }

    public void close() {
        ChannelFuture closeFuture = channel.close();
        try {
            closeFuture.sync();
        } catch (InterruptedException e) {
            logger.error("断开连接失败：线程被中断");
        }
        onClose();
    }

    public boolean isConnected() {
        return connected && channel.isActive();
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public int getMaxMissPongCount() {
        return maxMissPongCount;
    }

    public void setMaxMissPongCount(int maxMissPongCount) {
        this.maxMissPongCount = maxMissPongCount;
    }
}
