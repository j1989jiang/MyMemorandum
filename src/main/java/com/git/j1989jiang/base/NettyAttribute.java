package com.git.j1989jiang.base;

import io.netty.util.AttributeKey;

public class NettyAttribute {

	public static final AttributeKey<Long> CLIENT_ID = AttributeKey.newInstance("CLIENT_ID");
	public static final AttributeKey<Boolean> RECEIVED_PONG = AttributeKey.newInstance("RECEIVED_PONG");
	public static final AttributeKey<Integer> PONG_MISS_COUNT = AttributeKey.newInstance("PONG_MISS_COUNT");
}
