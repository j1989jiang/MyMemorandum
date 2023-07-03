package com.git.j1989jiang.test;

import com.git.j1989jiang.base.NettyWebSocketClientManager;
import com.git.j1989jiang.client.NettyWebSocketClientDemo;

public class NettyWebSocketClientTest {

    public static void main(String[] args) {
        NettyWebSocketClientDemo demo = new NettyWebSocketClientDemo(NettyWebSocketClientManager.generateClientId(), "", false);
        demo.connect();
        demo.sendMessage("");
        demo.close();
    }
}
