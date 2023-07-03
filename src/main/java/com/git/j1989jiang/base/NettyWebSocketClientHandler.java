package com.git.j1989jiang.base;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * WebSocket消息处理handler
 */
public class NettyWebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyWebSocketClientHandler.class);

    WebSocketClientHandshaker handshaker;

    ChannelPromise handshakeFuture;

    // 消息截取发送缓存区
    StringBuffer msgBuffer = new StringBuffer();

    public void handlerAdded(ChannelHandlerContext ctx) {
        this.handshakeFuture = ctx.newPromise();
    }

    public void setHandshaker(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public ChannelFuture handshakeFuture() {
        return this.handshakeFuture;
    }

    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

        Channel ch = ctx.channel();

        long clientId = ch.attr(NettyAttribute.CLIENT_ID).get();
        AbstractNettyWebSocketClient client = NettyWebSocketClientManager.getClient(clientId);

        FullHttpResponse response;
        if (!this.handshaker.isHandshakeComplete()) {
            try {
                response = (FullHttpResponse) msg;
                //握手协议返回，设置结束握手
                this.handshaker.finishHandshake(ch, response);
                //设置成功
                this.handshakeFuture.setSuccess();
                client.onOpen(response.headers());
            } catch (WebSocketHandshakeException var7) {
                var7.printStackTrace();
                FullHttpResponse res = (FullHttpResponse) msg;
                String errorMsg = String.format("WebSocket Client failed to connect,status:%s,reason:%s", res.status(), res.content().toString(CharsetUtil.UTF_8));
                this.handshakeFuture.setFailure(new Exception(errorMsg));
            }
        } else if (msg instanceof FullHttpResponse) {
            response = (FullHttpResponse) msg;
            throw new IllegalStateException("Unexpected FullHttpResponse (getStatus=" + response.status() + ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        } else {
            WebSocketFrame frame = (WebSocketFrame) msg;

            if (frame instanceof PingWebSocketFrame) {
                //服务端心跳请求消息
                client.onPing();
            } else if (frame instanceof PongWebSocketFrame) {
                //服务端心跳响应消息
                client.onPong();
            } else if (frame instanceof TextWebSocketFrame) {
                TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
                if (frame.isFinalFragment()) {
                    client.onMessage(textFrame.text());
                } else {
                    // 此处存疑，TextWebSocketFrame类型的消息isFinalFragment()方法应该永远返回true
                    msgBuffer.append(textFrame.text());
                }
            } else if (frame instanceof BinaryWebSocketFrame) {
                BinaryWebSocketFrame binFrame = (BinaryWebSocketFrame) frame;
                client.onMessage(binFrame.content());
            } else if (frame instanceof CloseWebSocketFrame) {
                ChannelFuture future = ch.close();
                future.sync();
                client.onClose();
            } else if (frame instanceof ContinuationWebSocketFrame) {
                ContinuationWebSocketFrame continuationWebSocketFrame = (ContinuationWebSocketFrame) frame;
                msgBuffer.append(continuationWebSocketFrame.text());
                if (frame.isFinalFragment()) {
                    client.onMessage(msgBuffer.toString());
                    msgBuffer.setLength(0);
                }
            }
        }
    }


    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            String msg = cause.getMessage();
            if (msg == null) msg = "it'sNull";
            if (msg.contains("连接超时")) {
                LOGGER.warn("IO异常A {} {}", ctx, msg);
            } else if (msg.contains("远程主机强迫关闭了一个现有的连接")) {
                //客户端session主动关闭的时候不再刷错误日志
                LOGGER.info("服务器主动关闭连接");
            } else {
                LOGGER.warn("IO异常 {} {}", ctx, cause);
            }
        }

        //打印错误堆栈，便于追踪
//        cause.printStackTrace();

        Channel ch = ctx.channel();
        long clientId = ch.attr(NettyAttribute.CLIENT_ID).get();
        AbstractNettyWebSocketClient client = NettyWebSocketClientManager.getClient(clientId);
        client.onClose();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }


}
