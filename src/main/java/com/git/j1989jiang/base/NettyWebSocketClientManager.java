package com.git.j1989jiang.base;


import com.git.j1989jiang.util.DelayUtils;
import com.git.j1989jiang.util.MyThreadPoolExecutor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class NettyWebSocketClientManager {

    private static final Map<Long, AbstractNettyWebSocketClient> CLIENT_MAP = Collections.synchronizedMap(new HashMap<>());

    private static MyThreadPoolExecutor MyThreadPoolExecutor = new MyThreadPoolExecutor(10, 100, 60, TimeUnit.SECONDS, new LinkedBlockingDeque<>(30000));

    private static AtomicLong idGenerator = new AtomicLong(0);

    // 使用一个线程来发送所有客户端的心跳请求，避免每一个客户端启动一个心跳线程导致线程占用过多
    private static Thread pingThread;

    // 心跳延迟，默认30秒一次心跳请求
    private static int pingDelay = 30;

    public static void addClient(AbstractNettyWebSocketClient nettyWebSocketClient) {
        synchronized (CLIENT_MAP) {
            CLIENT_MAP.put(nettyWebSocketClient.clientId, nettyWebSocketClient);
        }
        if (pingThread == null) {
            pingThread = new Thread(() -> {
                while (true) {
                    sendPingForAllClient();
                    DelayUtils.delay(pingDelay, TimeUnit.SECONDS);
                }
            });
            pingThread.start();
        }
    }

    public static AbstractNettyWebSocketClient getClient(long clientId) {
        return CLIENT_MAP.get(clientId);
    }

    public static int getClientCount() {
        return CLIENT_MAP.size();
    }

    public static Iterable<AbstractNettyWebSocketClient> getAllClients() {
        return CLIENT_MAP.values();
    }

    public static long generateClientId() {
        return idGenerator.incrementAndGet();
    }

    public static void sendPingForAllClient() {
        synchronized (CLIENT_MAP) {
            for (AbstractNettyWebSocketClient client : CLIENT_MAP.values()) {
                if (client.isConnected()) {
                    MyThreadPoolExecutor.execute(client::ping);
                }
            }
        }
    }

    public static int getPingDelay() {
        return pingDelay;
    }

    public static void setPingDelay(int pingDelay) {
        NettyWebSocketClientManager.pingDelay = pingDelay;
    }
}
